package model

import play.api.libs.json.{Json, Writes}

import java.time.Duration

case class Trip(destination: String, outbound: Flight, inbound: Flight) {
  def timeAtDestination: Long =
    Duration.between(outbound.arrivalTime, inbound.departureTime).toHours

  def totalPrice: Double =
    outbound.price + inbound.price

  def pricePerHour: Double =
    if (timeAtDestination > 0) totalPrice / timeAtDestination else totalPrice
}

object Trip {
  implicit val tripWrites: Writes[Trip] = (trip: Trip) => {
    Json.obj(
      "destination" -> trip.destination,
      "outbound" -> Json.toJson(trip.outbound),
      "inbound" -> Json.toJson(trip.inbound),
      "timeAtDestination" -> Json.toJson(trip.timeAtDestination),
      "totalPrice" -> Json.toJson(trip.totalPrice)
    )
  }
}