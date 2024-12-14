package services

import model.Trip

class TripCreator(flightService: FlightService) {

  def create(fromCode: String, toCode: String, date: String): List[Trip] = {
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
