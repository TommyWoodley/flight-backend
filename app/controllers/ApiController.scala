package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.{AirportService, FlightService, HttpApiService, TripCreator}

import java.time.LocalDate
import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ApiController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private val tripCreator = new TripCreator(new FlightService(new HttpApiService, new AirportService))

  def getApiData: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val trips = tripCreator.create("LHR", "CDG", LocalDate.of(2025, 1, 18), 2)
    val jsonResponse = Json.toJson(trips)
    Ok(jsonResponse)
  }

}
