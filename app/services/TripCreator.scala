package services

import model.Trip

class TripCreator(flightCache: FlightService) {

  def create(fromCode: String, toCode: String, date: String): List[Trip] = {
    val outboundFlights = flightCache.getFlights(fromCode, toCode, date)

    val inboundFlights = flightCache.getFlights(toCode, fromCode, date)

    for {
      outbound <- outboundFlights
      inbound <- inboundFlights
      if inbound.departureTime.isAfter(outbound.arrivalTime)
    } yield Trip(toCode, outbound, inbound)
  }
}
