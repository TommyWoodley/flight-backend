package cache

import model.Airport
import play.api.libs.json.Json

import scala.io.Source

class AirportCache {
  private val airports: List[Airport] = {
    val source = Source.fromFile("conf/airports.json")
    val jsonString = try source.mkString finally source.close()
    val json = Json.parse(jsonString)
    json.as[List[Airport]]
  }

  def allAirports: List[Airport] = airports

}
