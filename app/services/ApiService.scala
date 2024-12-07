package services

import play.api.libs.json.JsValue

abstract class ApiService {
  def fetchAirportsForCity(cityCode: String): JsValue
}
