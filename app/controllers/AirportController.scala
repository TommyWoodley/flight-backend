package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import services.AirportService

@Singleton
class AirportController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
  private val airportService = new AirportService()

  def getAllAirports(): Action[AnyContent] = Action { implicit request =>
    val airports = airportService.allAirports
    Ok(Json.toJson(airports))
  }
}
