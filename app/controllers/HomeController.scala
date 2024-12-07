package controllers

import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def getApiData() = Action { implicit request: Request[AnyContent] =>
    val jsonResponse = Json.obj(
      "status" -> "success",
      "message" -> "This is a sample API response"
    )
    Ok(jsonResponse)
  }

}
