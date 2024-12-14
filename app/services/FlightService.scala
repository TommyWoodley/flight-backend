package services

import model.Flight
import play.api.libs.json.JsValue
import services.FlightService.{RetrieveFlightsEndpoint, RetrieveFlightsIncompleteEndpoint}

class FlightService(apiService: ApiService, airportService: AirportService) {
  def getFlights(fromCode: String, toCode: String, date: String): List[Flight] = {
    val fromAirport = airportService.getAirport(fromCode)
    val toAirport = airportService.getAirport(toCode)

    val params = Map(
      "originSkyId" -> fromAirport.map(_._1).getOrElse(""),
      "destinationSkyId" -> toAirport.map(_._1).getOrElse(""),
      "originEntityId" -> fromAirport.map(_._2).getOrElse(""),
      "destinationEntityId" -> toAirport.map(_._2).getOrElse(""),
      "date" -> date
    )

    var jsonResponse = apiService.get(RetrieveFlightsEndpoint, params)
    var status = (jsonResponse \ "context" \ "status").asOpt[String].getOrElse("complete")
    val responses = scala.collection.mutable.ListBuffer[JsValue](jsonResponse)

    while (status == "incomplete") {
      val sessionId = (jsonResponse \ "context" \ "sessionId").as[String]
      val incompleteParams = Map("sessionId" -> sessionId)
      jsonResponse = apiService.get(RetrieveFlightsIncompleteEndpoint, incompleteParams)
      status = (jsonResponse \ "status").asOpt[String].getOrElse("complete")
      responses += jsonResponse
    }

    responses.flatMap(mapItinerariesToFlights).toList
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

object FlightService {
  val RetrieveFlightsEndpoint = "/retrieveFlights"
  val RetrieveFlightsIncompleteEndpoint = "/retrieveFlightsIncomplete"
}