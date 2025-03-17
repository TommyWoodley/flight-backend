package controllers

import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, DateService, FlightService, TripCreator, WeekendService, AlternativeTripService}
import model.{Flight, Trip}
import play.api.Logger
import validators.RequestValidator

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
    alternativeTripService: AlternativeTripService,
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

          try {
            val bestTrips = weekendService.getWeekendTrips(fromCodes, month, year, numberOfExtraDays)
            Ok(Json.toJson(bestTrips))
          } catch {
            case e: IllegalArgumentException =>
              BadRequest(e.getMessage)
            case e: Exception                =>
              BadRequest(e.getMessage)
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

  def findTripOptions: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asJson
      .map { json =>
        try {
          val trip = json.as[Trip]

          // Extract the required information
          val fromCode       = trip.outbound.departureCode
          val departureMonth = trip.outbound.departureTime.getMonthValue
          val departureYear  = trip.outbound.departureTime.getYear
          val numberOfExtraDays = {
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
              trip.outbound.departureTime.toLocalDate,
              trip.inbound.arrivalTime.toLocalDate
            )
            daysBetween.toInt - 1 // -1 because we don't count the departure day
          }
          val arrivalCode    = trip.inbound.arrivalCode

          // Log all the information
          logger.info(s"Received trip: $trip")
          logger.info(s"Extracted information:")
          logger.info(s"  From airport code: $fromCode")
          logger.info(s"  Month: $departureMonth")
          logger.info(s"  Year: $departureYear")
          logger.info(s"  Number of extra days: $numberOfExtraDays")

          Ok(
            Json.obj(
              "message"       -> "Trip logged successfully",
              "extractedInfo" -> Json.obj(
                "fromCode"          -> fromCode,
                "month"             -> departureMonth,
                "year"              -> departureYear,
                "numberOfExtraDays" -> numberOfExtraDays,
                "arrivalCode"       -> arrivalCode
              )
            )
          )
        } catch {
          case e: Exception =>
            BadRequest(Json.obj("error" -> s"Invalid trip data: ${e.getMessage}"))
        }
      }
      .getOrElse {
        BadRequest(Json.obj("error" -> "Expecting JSON data"))
      }
  }

  def getAlternativeTrips: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val originOpt       = request.getQueryString("origin")
    val destinationOpt  = request.getQueryString("destination")
    val monthOpt        = request.getQueryString("month")
    val extraDaysOpt    = request.getQueryString("extra_days")
    val departureDayOpt = request.getQueryString("departure_day")

    // Validate required parameters
    if (originOpt.isEmpty) {
      BadRequest("Missing required query parameter: origin")
    } else if (destinationOpt.isEmpty) {
      BadRequest("Missing required query parameter: destination")
    } else if (monthOpt.isEmpty) {
      BadRequest("Missing required query parameter: month")
    } else if (extraDaysOpt.isEmpty) {
      BadRequest("Missing required query parameter: extra_days")
    } else if (departureDayOpt.isEmpty) {
      BadRequest("Missing required query parameter: departure_day")
    } else {
      try {
        // Parse and validate extra_days
        val extraDays = extraDaysOpt.get.toInt
        if (extraDays < 0 || extraDays > 1) {
          BadRequest("extra_days must be 0 (for weekend) or 1 (for long-weekend)")
        } else {
          try {
            // Validate month format (YYYY-MM)
            val month = monthOpt.get
            if (!month.matches("\\d{4}-\\d{2}")) {
              BadRequest("Invalid month format. Please use YYYY-MM format.")
            } else {
              try {
                // Validate departure_day format (YYYY-MM-DD)
                val departureDay = departureDayOpt.get
                if (!departureDay.matches("\\d{4}-\\d{2}-\\d{2}")) {
                  BadRequest("Invalid departure_day format. Please use YYYY-MM-DD format.")
                } else {
                  try {
                    // Get alternative trips
                    val alternativeTrips = alternativeTripService.getAlternativeTrips(
                      originOpt.get,
                      destinationOpt.get,
                      month,
                      extraDays,
                      departureDay
                    )
                    Ok(Json.toJson(alternativeTrips))
                  } catch {
                    case e: IllegalArgumentException =>
                      BadRequest(e.getMessage)
                    case e: Exception                =>
                      logger.error("Error getting alternative trips", e)
                      InternalServerError(s"Error getting alternative trips: ${e.getMessage}")
                  }
                }
              } catch {
                case e: DateTimeParseException =>
                  BadRequest("Invalid departure_day format. Please use YYYY-MM-DD format.")
                case e: Exception              =>
                  logger.error("Error parsing departure_day", e)
                  BadRequest(s"Error parsing departure_day: ${e.getMessage}")
              }
            }
          } catch {
            case e: DateTimeParseException =>
              BadRequest("Invalid month format. Please use YYYY-MM format.")
            case e: Exception              =>
              logger.error("Error parsing month", e)
              BadRequest(s"Error parsing month: ${e.getMessage}")
          }
        }
      } catch {
        case _: NumberFormatException =>
          BadRequest("Invalid number format for extra_days parameter.")
        case e: Exception             =>
          logger.error("Error parsing extra_days", e)
          BadRequest(s"Error parsing extra_days: ${e.getMessage}")
      }
    }
  }
}
