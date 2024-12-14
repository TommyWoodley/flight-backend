package services

import model.Airport
import play.api.libs.json.Json

import scala.io.Source

class AirportService {
  private val airports: List[Airport] = {
    val source = Source.fromFile("conf/airports.json")
    val jsonString = try source.mkString finally source.close()
    val json = Json.parse(jsonString)
    json.as[List[Airport]]
  }

  def allAirports: List[Airport] = airports

  def getAirport(iata: String): Option[(String, String)] = airports
    .find(_.code == iata)
    .map(a => (a.skyId, a.entity))

}
