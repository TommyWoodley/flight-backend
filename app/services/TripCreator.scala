package services

import model.{Airport, Flight, Trip}
import play.api.Logger

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class TripCreator(flightService: FlightService, airportService: AirportService) {
  private val logger: Logger                              = Logger(this.getClass)
  private implicit val executor: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  def create(fromCode: String, date: LocalDate, numberOfDays: Int): List[Trip] = {
    create(List(fromCode), date, numberOfDays)
  }

  def create(fromCode: List[String], outboundDate: LocalDate, numberOfDays: Int): List[Trip] = {
    logger.info(s"Creating trips from $fromCode on $outboundDate")

    val outboundAirports    = airportService.getAirportsByCode(fromCode)
    val destinationAirports = airportService.getAllAirportsInADifferentCountry(outboundAirports.head.country)

    val departureDate = outboundDate.plusDays(numberOfDays)

    val trips: List[Trip] = createAllTrips(outboundDate, outboundAirports, destinationAirports, departureDate)

    trips
      .groupBy(_.destination)
      .values
      .flatMap { trips =>
        trips.sortBy(_.pricePerHour).headOption
      }
      .toList
      .sortBy(_.totalPrice)
  }

  private def createAllTrips(
      outboundDate: LocalDate,
      outboundAirports: List[Airport],
      destinationAirports: List[Airport],
      departureDate: LocalDate
  ): List[Trip] = {
    val outboundFlightsFutures = createFlightFutures(outboundDate, outboundAirports, destinationAirports)

    val inboundFlightsFutures = createFlightFutures(departureDate, destinationAirports, outboundAirports)

    val tripsFuture = for {
      outboundFlightsLists <- outboundFlightsFutures
      inboundFlightsLists  <- inboundFlightsFutures
    } yield {
      val outboundFlights = outboundFlightsLists.flatten
      val inboundFlights  = inboundFlightsLists.flatten

      for {
        outbound          <- outboundFlights
        inbound           <- inboundFlights
        if outbound.arrivalCode == inbound.departureCode && inbound.departureTime.isAfter(outbound.arrivalTime)
        destinationAirport = destinationAirports.find(_.code == outbound.arrivalCode).get
      } yield Trip(destinationAirport.country, outbound, inbound)
    }

    val trips = Await.result(tripsFuture, 60.seconds)
    trips
  }

  private def createFlightFutures(date: LocalDate, fromAirports: List[Airport], toAirports: List[Airport]) =
    Future.sequence {
      for {
        from <- fromAirports
        to   <- toAirports
      } yield Future {
        flightService.getFlights(from, to, date)
      }
    }
}
