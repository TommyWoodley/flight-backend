package controllers

import cache.CachingApiService
import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, FlightService, HttpApiService, TripCreator}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject._

@Singleton
class ApiController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val airportService = new AirportService
  private val tripCreator = new TripCreator(new FlightService(new CachingApiService(new HttpApiService)), airportService)

  def getApiData: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val fromCodeOpt = request.getQueryString("fromCode")
    val dateStrOpt = request.getQueryString("date")
    val numberOfDaysStrOpt = request.getQueryString("numberOfDays")

    (fromCodeOpt, dateStrOpt, numberOfDaysStrOpt) match {
      case (Some(fromCode), Some(dateStr), Some(numberOfDaysStr)) =>
        try {
          val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
          val numberOfDays = numberOfDaysStr.toInt

          val trips = tripCreator.create(fromCode, date, numberOfDays)
          val jsonResponse = Json.toJson(trips)
          Ok(jsonResponse)
        } catch {
          case _: DateTimeParseException =>
            BadRequest("Invalid date format. Please use ISO_LOCAL_DATE format (yyyy-MM-dd).")
        }

      case _ =>
        BadRequest("Missing required query parameters: fromCode, date, and/or numberOfDays")
    }
  }
}