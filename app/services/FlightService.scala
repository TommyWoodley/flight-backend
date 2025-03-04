package services

import model.{Airport, Flight}
import play.api.Logger
import play.api.libs.json.JsValue
import services.FlightService.{RetrieveFlightsEndpoint, RetrieveFlightsIncompleteEndpoint}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.collection.mutable
import javax.inject.Inject
import scala.util.{Success, Failure}

class FlightService @Inject() (apiService: ApiService, dynamoDBService: DynamoDBService) {
  private val logger: Logger = Logger(this.getClass)

  def getFlights(from: Airport, to: Airport, date: LocalDate): List[Flight] = {
    logger.info(s"Getting flights for ${from.skyId} to ${to.skyId} on $date")
    // First try to get flights from the database
    dynamoDBService.getFlights(from, to, date) match {
      case Success(flights) if flights.nonEmpty =>
        logger.info(s"Retrieved ${flights.length} flights from database for ${from.skyId} to ${to.skyId} on $date")
        flights

      case _ =>
        // If no flights found in database or error occurred, fetch from API
        logger.info(s"Fetching flights from API for ${from.skyId} to ${to.skyId} on $date")
        val flights = fetchFlightsFromApi(from, to, date)

        // Store the flights in the database
        dynamoDBService.storeFlights(flights, date) match {
          case Success(_) =>
            logger.info(s"Stored ${flights.length} flights in database")
          case Failure(e) =>
            logger.error(s"Failed to store flights in database: ${e.getMessage}")
        }

        flights
    }
  }

  private def fetchFlightsFromApi(from: Airport, to: Airport, date: LocalDate): List[Flight] = {
    logger.info(s"Fetching flights from API for ${from.skyId} to ${to.skyId} on $date")
    val params = Map(
      "originSkyId"         -> from.skyId,
      "destinationSkyId"    -> to.skyId,
      "originEntityId"      -> from.entity,
      "destinationEntityId" -> to.entity,
      "date"                -> date.format(DateTimeFormatter.ISO_LOCAL_DATE),
      "currency"            -> "GBP"
    )

    var jsonResponseOpt = apiService.get(RetrieveFlightsEndpoint, params)
    fetchIncompleteFlights(jsonResponseOpt)
  }

  @tailrec
  private def fetchIncompleteFlights(jsonResponseOpt: Option[JsValue], acc: List[Flight] = List.empty): List[Flight] = {
    jsonResponseOpt match {
      case Some(jsonResponse) =>
        val status      = (jsonResponse \ "context" \ "status").as[String]
        val itineraries = (jsonResponse \ "itineraries").as[List[JsValue]]
        status match {
          case "complete"   => acc ++ mapItinerariesToFlights(itineraries)
          case "incomplete" =>
            val sessionId             = (jsonResponse \ "context" \ "sessionId").as[String]
            val incompleteParams      = Map("sessionId" -> sessionId)
            val incompleteResponseOpt = apiService.get(RetrieveFlightsIncompleteEndpoint, incompleteParams)
            val updatedAcc            = acc ++ mapItinerariesToFlights(itineraries)
            fetchIncompleteFlights(incompleteResponseOpt, updatedAcc)
          case _            =>
            logger.warn(s"Unknown status: $status")
            acc
        }
      case None               =>
        logger.warn("No JSON response")
        acc
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
