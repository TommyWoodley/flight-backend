package controllers

import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, DateService, FlightService, TripCreator, WeekendService}
import model.{Flight, Trip}
import play.api.Logger
import validators.RequestValidator

import java.time.{LocalDate, Year, YearMonth}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject._
import scala.util.Success
import scala.util.Failure

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

    RequestValidator.validateTripRequest(fromCodeOpt, dateStrOpt, numberOfDaysStrOpt) match {
      case Failure(exception)                       =>
        BadRequest(exception.getMessage)
      case Success((fromCodes, date, numberOfDays)) =>
        try {
          val trips = tripCreator.create(fromCodes, date, numberOfDays)
          Ok(Json.toJson(trips))
        } catch {
          case e: Exception => BadRequest(e.getMessage)
        }
    }
  }

  def getWeekends: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val fromCodeOpt             = request.getQueryString("fromCode")
    val monthStrOpt             = request.getQueryString("month")
    val yearStrOpt              = request.getQueryString("year")
    val numberOfExtraDaysStrOpt = request.getQueryString("numberOfExtraDays")

    RequestValidator.validateWeekendRequest(fromCodeOpt, monthStrOpt, yearStrOpt, numberOfExtraDaysStrOpt) match {
      case Failure(exception)                                   =>
        BadRequest(exception.getMessage)
      case Success((fromCodes, month, year, numberOfExtraDays)) =>
        try {
          val bestTrips = weekendService.getWeekendTrips(fromCodes, month, year, numberOfExtraDays)
          Ok(Json.toJson(bestTrips))
        } catch {
          case e: IllegalArgumentException =>
            BadRequest(e.getMessage)
          case e: Exception                =>
            BadRequest(e.getMessage)
        }
    }
  }
}
