package services

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

class LocalApiService extends ApiService {
  override def fetchAirportsForCity(cityCode: String): JsValue = {
    val jsonFilePath = "httpRequests/flightLabs/getCities" + cityCode + ".json"
    val source = Source.fromFile(jsonFilePath)
    val jsonString = try source.mkString finally source.close()
    Json.parse(jsonString)
  }
}
