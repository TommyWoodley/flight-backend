package services

import model.Trip

class TripCreator(flightCache: FlightCache) {

  def create(fromCode: String, toCode: String): List[Trip] = {
    val outboundFlights = flightCache.getFlights(fromCode, toCode)

    val inboundFlights = flightCache.getFlights(toCode, fromCode)

    for {
      outbound <- outboundFlights
      inbound <- inboundFlights
      if inbound.departureTime.isAfter(outbound.arrivalTime)
    } yield Trip(toCode, outbound, inbound)
  }
}
