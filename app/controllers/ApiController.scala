package controllers

import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, DateService, FlightService, TripCreator, WeekendService}
import model.{Flight, Trip}
import play.api.Logger

import java.time.{LocalDate, Year, YearMonth}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject._

@Singleton
class ApiController @Inject() (
    val controllerComponents: ControllerComponents,
    airportService: AirportService,
    dateService: DateService,
    flightService: FlightService,
    weekendService: WeekendService,
    tripCreator: TripCreator,
    implicit val config: Configuration
) extends BaseController {

  private val logger = Logger(this.getClass)

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

          val bestTrips = weekendService.getWeekendTrips(fromCodes, month, year, numberOfExtraDays)
          Ok(Json.toJson(bestTrips))
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

  def findTripOptions: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asJson
      .map { json =>
        try {
          val trip = json.as[Trip]
          logger.info(s"Received trip: $trip")
          logger.info(s"Destination: ${trip.destination}")
          logger.info(s"Outbound flight: ${trip.outbound}")
          logger.info(s"Inbound flight: ${trip.inbound}")
          logger.info(s"Time at destination: ${trip.timeAtDestination} hours")
          logger.info(s"Total price: £${trip.totalPrice}")
          logger.info(s"Price per hour: £${trip.pricePerHour}")
          Ok(Json.obj("message" -> "Trip logged successfully"))
        } catch {
          case e: Exception =>
            BadRequest(Json.obj("error" -> s"Invalid trip data: ${e.getMessage}"))
        }
      }
      .getOrElse {
        BadRequest(Json.obj("error" -> "Expecting JSON data"))
      }
  }
}
