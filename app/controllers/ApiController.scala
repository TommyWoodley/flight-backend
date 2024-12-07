package controllers

import cache.FlightCache
import play.api.libs.json.Json
import play.api.mvc._
import services.TripCreator

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ApiController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private val tripCreator = new TripCreator(new FlightCache)

  def getApiData: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val trips = tripCreator.create("LHR", "CDG")
    val jsonResponse = Json.toJson(trips)
    Ok(jsonResponse)
  }

}
