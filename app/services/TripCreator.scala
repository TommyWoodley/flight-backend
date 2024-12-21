package services

import model.Trip
import play.api.Logger

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class TripCreator(flightService: FlightService) {
  private val logger: Logger = Logger(this.getClass)
  private implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

  def create(fromCode: String, toCode: String, date: LocalDate, numberOfDays: Int): List[Trip] = {
    logger.info(s"Creating trips from $fromCode to $toCode on $date")

    val outboundFlightsFuture = Future {
      flightService.getFlights(fromCode, toCode, date)
    }
    val inboundFlightsFuture = Future {
      flightService.getFlights(toCode, fromCode, date.plusDays(numberOfDays))
    }

    val tripsFuture = for {
      outboundFlights <- outboundFlightsFuture
      inboundFlights <- inboundFlightsFuture
    } yield {
      for {
        outbound <- outboundFlights
        inbound <- inboundFlights
        if inbound.departureTime.isAfter(outbound.arrivalTime)
      } yield Trip(toCode, outbound, inbound)
    }

    Await.result(tripsFuture, 30.seconds).sortBy(_.pricePerHour).take(10)
  }
}
