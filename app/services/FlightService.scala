package services

import model.{Airport, Flight}
import play.api.Logger
import play.api.libs.json.JsValue
import services.FlightService.{RetrieveFlightsEndpoint, RetrieveFlightsIncompleteEndpoint}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.collection.mutable

class FlightService(apiService: ApiService) {
  private val logger: Logger = Logger(this.getClass)

  def getFlights(from: Airport, to: Airport, date: LocalDate): List[Flight] = {
    val params = Map(
      "originSkyId"         -> from.skyId,
      "destinationSkyId"    -> to.skyId,
      "originEntityId"      -> from.entity,
      "destinationEntityId" -> to.entity,
      "date"                -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )

    var jsonResponseOpt = apiService.get(RetrieveFlightsEndpoint, params)

    fetchIncompleteFlights(jsonResponseOpt)
  }

  private def fetchIncompleteFlights(jsonResponseOpt: Option[JsValue]): List[Flight] = {
    jsonResponseOpt match {
      case Some(jsonResponse) =>
        val status      = (jsonResponse \ "context" \ "status").as[String]
        val itineraries = (jsonResponse \ "itineraries").as[List[JsValue]]
        status match {
          case "complete"   => mapItinerariesToFlights(itineraries)
          case "incomplete" =>
            val sessionId             = (jsonResponse \ "context" \ "sessionId").as[String]
            val incompleteParams      = Map("sessionId" -> sessionId)
            val incompleteResponseOpt = apiService.get(RetrieveFlightsIncompleteEndpoint, incompleteParams)

            fetchIncompleteFlights(incompleteResponseOpt) ++ mapItinerariesToFlights(itineraries)
          case _            =>
            logger.warn(s"Unknown status: $status")
            Nil
        }
      case None               =>
        logger.warn("No JSON response")
        Nil
    }
  }

  private def mapItinerariesToFlights(itineraries: List[JsValue]) = {
    itineraries
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
  val RetrieveFlightsEndpoint           = "/retrieveFlights"
  val RetrieveFlightsIncompleteEndpoint = "/retrieveFlightsIncomplete"
}
