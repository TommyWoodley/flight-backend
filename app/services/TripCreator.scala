package services

import model.Trip
import play.api.Logger

class TripCreator(flightService: FlightService) {
  private val logger: Logger = Logger(this.getClass)

  def create(fromCode: String, toCode: String, date: String): List[Trip] = {
    logger.info(s"Creating trips from $fromCode to $toCode on $date")
    val outboundFlights = flightService.getFlights(fromCode, toCode, date)

    val inboundFlights = flightService.getFlights(toCode, fromCode, date)

    val trips = for {
      outbound <- outboundFlights
      inbound <- inboundFlights
      if inbound.departureTime.isAfter(outbound.arrivalTime)
    } yield Trip(toCode, outbound, inbound)

    trips.sortBy(_.timeAtDestination).reverse.take(10)
  }
}
