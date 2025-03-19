package controllers

import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._

@Singleton
class HealthController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
  def health: Action[AnyContent] = Action {
    Ok(Json.obj("healthy" -> true))
  }
}
