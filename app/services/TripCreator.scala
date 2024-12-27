package services

import model.Trip
import play.api.Logger

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class TripCreator(flightService: FlightService, airportService: AirportService) {
  private val logger: Logger = Logger(this.getClass)
  private implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  def create(fromCode: String, date: LocalDate, numberOfDays: Int): List[Trip] = {
    logger.info(s"Creating trips from $fromCode on $date")

    val outboundAirport = airportService.getAirportByCode(fromCode)
    val destinationAirports = airportService.getAllAirportsInADifferentCountry(outboundAirport.country)

    val tripsFutures = destinationAirports.map { inboundAirport =>
      val outboundFlightsFuture = Future {
        flightService.getFlights(outboundAirport, inboundAirport, date)
      }
      val inboundFlightsFuture = Future {
        flightService.getFlights(inboundAirport, outboundAirport, date.plusDays(numberOfDays))
      }

      for {
        outboundFlights <- outboundFlightsFuture
        inboundFlights <- inboundFlightsFuture
      } yield {
        for {
          outbound <- outboundFlights
          inbound <- inboundFlights
          if inbound.departureTime.isAfter(outbound.arrivalTime)
        } yield Trip(inboundAirport.country, outbound, inbound)
      }
    }

    val tripsFuture = Future.sequence(tripsFutures).map(_.flatten)
    val trips = Await.result(tripsFuture, 30.seconds)

    trips.groupBy(_.destination).values.flatMap { trips =>
      trips.sortBy(_.pricePerHour).headOption
    }.toList.sortBy(_.totalPrice)
  }
}
