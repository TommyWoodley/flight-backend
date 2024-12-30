package services

import model.{Airport, Flight}
import play.api.libs.json.JsValue
import services.FlightService.{RetrieveFlightsEndpoint, RetrieveFlightsIncompleteEndpoint}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FlightService(apiService: ApiService) {
  def getFlights(from: Airport, to: Airport, date: LocalDate): List[Flight] = {
    val params = Map(
      "originSkyId"         -> from.skyId,
      "destinationSkyId"    -> to.skyId,
      "originEntityId"      -> from.entity,
      "destinationEntityId" -> to.entity,
      "date"                -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )

    var jsonResponseOpt = apiService.get(RetrieveFlightsEndpoint, params)
    var status          = jsonResponseOpt.flatMap(json => (json \ "context" \ "status").asOpt[String]).getOrElse("complete")
    val responses       = scala.collection.mutable.ListBuffer[JsValue]()

    jsonResponseOpt.foreach(responses += _)

    while (status == "incomplete") {
      val sessionIdOpt = jsonResponseOpt.flatMap(json => (json \ "context" \ "sessionId").asOpt[String])
      sessionIdOpt match {
        case Some(sessionId) =>
          val incompleteParams = Map("sessionId" -> sessionId)
          jsonResponseOpt = apiService.get(RetrieveFlightsIncompleteEndpoint, incompleteParams)
          status = jsonResponseOpt.flatMap(json => (json \ "status").asOpt[String]).getOrElse("complete")
          jsonResponseOpt.foreach(responses += _)
        case None            =>
          status = "complete"
      }
    }

    responses.flatMap(mapItinerariesToFlights).toList.distinct
  }

  private def mapItinerariesToFlights(jsonResponse: JsValue) = {
    (jsonResponse \ "itineraries").asOpt[List[JsValue]] match {
      case Some(itineraries) =>
        itineraries
          .map(itinerary => ((itinerary \ "legs").as[List[JsValue]], (itinerary \ "price" \ "raw").as[Double]))
          .filter { case (legs, _) => legs.length == 1 }
          .map { case (legs, price) => (legs.head, price) }
          .map { case (leg, price) => ((leg \ "segments").as[List[JsValue]], price) }
          .filter { case (segments, _) => segments.length == 1 }
          .map { case (segments, price) => (segments.head, price) }
          .map { case (segment, price) => segment.as[Flight].copy(price = price) }
      case None              => List.empty[Flight]
    }
  }
}

object FlightService {
  val RetrieveFlightsEndpoint           = "/retrieveFlights"
  val RetrieveFlightsIncompleteEndpoint = "/retrieveFlightsIncomplete"
}
