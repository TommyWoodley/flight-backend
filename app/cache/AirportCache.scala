package cache

import model.Airport
import play.api.libs.json.JsValue
import services.ApiService

class AirportCache(apiService: ApiService) {
  private val cities = List("LON")
  private val airports: List[Airport] = cities.flatMap { cityCode =>
    val json: JsValue = apiService.fetchAirportsForCity(cityCode)
    (json \ "data").as[List[JsValue]]
      .filter(airportJson => (airportJson \ "iata_code").asOpt[String].isDefined)
      .map(_.as[Airport])
  }

  def allAirports: List[Airport] = airports

}
