package services

import play.api.libs.json.JsValue

abstract class ApiService {
  def get(endpoint: String, params: Map[String, String]): JsValue = ???

}
