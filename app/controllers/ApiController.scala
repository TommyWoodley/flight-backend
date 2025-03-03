package controllers

import cache.CachingApiService
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, DateService, FlightService, HttpApiService, TripCreator}

import java.time.{LocalDate, Year, YearMonth}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject._

@Singleton
class ApiController @Inject() (val controllerComponents: ControllerComponents, implicit val config: Configuration)
    extends BaseController {
  protected val airportService = new AirportService
  protected val dateService    = new DateService
  protected val tripCreator    =
    new TripCreator(new FlightService(new CachingApiService(new HttpApiService)), airportService)

  def getTrips: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val fromCodeOpt        = request.getQueryString("fromCode")
    val dateStrOpt         = request.getQueryString("date")
    val numberOfDaysStrOpt = request.getQueryString("numberOfDays")

    if (fromCodeOpt.isEmpty) {
      BadRequest("Missing required query parameter: fromCode")
    } else {
      val fromCodes = if (fromCodeOpt.get.isEmpty) List.empty else fromCodeOpt.get.split(",").toList

      if (dateStrOpt.isDefined && numberOfDaysStrOpt.isDefined) {
        try {
          val date         = LocalDate.parse(dateStrOpt.get, DateTimeFormatter.ISO_LOCAL_DATE)
          val numberOfDays = numberOfDaysStrOpt.get.toInt
          val trips        = tripCreator.create(fromCodes, date, numberOfDays)
          Ok(Json.toJson(trips))
        } catch {
          case _: DateTimeParseException =>
            BadRequest("Invalid date format. Please use ISO_LOCAL_DATE format (yyyy-MM-dd).")
          case _: NumberFormatException  =>
            BadRequest("Invalid number format for numberOfDays parameter.")
          case e: Exception              =>
            BadRequest(e.getMessage)
        }
      } else {
        BadRequest("Must provide both date and numberOfDays parameters")
      }
    }
  }

  def getWeekends: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val fromCodeOpt             = request.getQueryString("fromCode")
    val monthStrOpt             = request.getQueryString("month")
    val yearStrOpt              = request.getQueryString("year")
    val numberOfExtraDaysStrOpt = request.getQueryString("numberOfExtraDays")

    if (fromCodeOpt.isEmpty) {
      BadRequest("Missing required query parameter: fromCode")
    } else {
      val fromCodes = if (fromCodeOpt.get.isEmpty) List.empty else fromCodeOpt.get.split(",").toList

      if (monthStrOpt.isDefined && yearStrOpt.isDefined && numberOfExtraDaysStrOpt.isDefined) {
        try {
          val month             = monthStrOpt.get.toInt
          val year              = yearStrOpt.get.toInt
          val numberOfExtraDays = numberOfExtraDaysStrOpt.get.toInt

          if (month < 1 || month > 12) {
            BadRequest("Month must be between 1 and 12")
          } else if (numberOfExtraDays > 2) {
            BadRequest("Additional days cannot be more than 2")
          } else {
            val tripDates = dateService.getWeekendTrips(month, year, numberOfExtraDays)

            // Find trips for each possible date combination and get the best ones
            val allTrips = tripDates.flatMap { tripDate =>
              tripCreator.create(
                fromCodes,
                tripDate.startDate,
                numberOfExtraDays + 1
              ) // +1 because numberOfExtraDays is additional to the weekend
            }

            // Group by destination and get the cheapest trip for each
            val bestTrips = allTrips
              .groupBy(_.destination)
              .values
              .flatMap(_.minByOption(_.totalPrice))
              .toList
              .sortBy(_.totalPrice)

            Ok(Json.toJson(bestTrips))
          }
        } catch {
          case _: NumberFormatException =>
            BadRequest("Invalid number format for month, year, or numberOfExtraDays parameters.")
          case e: Exception             =>
            BadRequest(e.getMessage)
        }
      } else {
        BadRequest("Must provide month, year, and numberOfExtraDays parameters")
      }
    }
  }
}
