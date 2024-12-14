package services

import model.Flight
import play.api.libs.json.JsValue

class FlightService(apiService: ApiService, airportService: AirportService) {
  def getFlights(fromCode: String, toCode: String): List[Flight] = {
    val fromAirport = airportService.getAirport(fromCode)
    val toAirport = airportService.getAirport(toCode)

    val params = Map(
      "originSkyId" -> fromAirport.map(_._1).getOrElse(""),
      "destinationSkyId" -> toAirport.map(_._1).getOrElse(""),
      "originEntityId" -> fromAirport.map(_._2).getOrElse(""),
      "destinationEntityId" -> toAirport.map(_._2).getOrElse(""),
      "date" -> "2024-12-13"
    )

    val jsonResponse = apiService.get("/retrieveFlights", params)

    mapItinerariesToFlights(jsonResponse)
  }

  private def mapItinerariesToFlights(jsonResponse: JsValue) = {
    (jsonResponse \ "itineraries").as[List[JsValue]]
      .map(itinerary => (itinerary \ "legs").as[List[JsValue]])
      .filter(legs => legs.length == 1)
      .map(legs => legs.head)
      .map(leg => (leg \ "segments").as[List[JsValue]])
      .filter(segments => segments.length == 1)
      .map(segments => segments.head)
      .map(segment => segment.as[Flight])
  }
}
