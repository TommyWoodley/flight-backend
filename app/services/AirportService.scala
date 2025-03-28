package services

import model.Airport
import play.api.libs.json.Json

import scala.io.Source

class AirportService {
  private val airports: List[Airport] = {
    val source     = Source.fromFile("conf/airports.json")
    val jsonString =
      try source.mkString
      finally source.close()
    val json       = Json.parse(jsonString)
    json.as[List[Airport]]
  }

  def allAirports: List[Airport] = airports

  def getAirport(iata: String): Option[(String, String)] =
    airports
      .find(_.code == iata)
      .map(a => (a.skyId, a.entity))

  def getAirportByCode(code: String): Airport =
    airports
      .find(_.code == code)
      .getOrElse(throw new NoSuchElementException(s"Airport with code $code not found"))

  def getAirportsByCode(codes: List[String]): List[Airport] =
    codes.map(getAirportByCode)

  def getAllAirportsInADifferentCountry(country: String): List[Airport] =
    airports.filter(_.country != country)

}
