package services

import model.{Airport, Flight}
import play.api.libs.json.JsValue
import services.FlightService.{RetrieveFlightsEndpoint, RetrieveFlightsIncompleteEndpoint}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FlightService(apiService: ApiService) {
  def getFlights(from: Airport, to: Airport, date: LocalDate): List[Flight] = {
    val params = Map(
      "originSkyId" -> from.skyId,
      "destinationSkyId" -> to.skyId,
      "originEntityId" -> from.entity,
      "destinationEntityId" -> to.entity,
      "date" -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
      .map(itinerary => ((itinerary \ "legs").as[List[JsValue]], (itinerary \ "price" \ "raw").as[Double]))
      .filter { case (legs, _) => legs.length == 1 }
      .map { case (legs, price) => (legs.head, price) }
      .map { case (leg, price) => ((leg \ "segments").as[List[JsValue]], price) }
      .filter { case (segments, _) => segments.length == 1 }
      .map { case (segments, price) => (segments.head, price) }
      .map { case (segment, price) => segment.as[Flight].copy(price = price) }
  }
}

object FlightService {
  val RetrieveFlightsEndpoint = "/retrieveFlights"
  val RetrieveFlightsIncompleteEndpoint = "/retrieveFlightsIncomplete"
}