package model

import play.api.libs.json.{Json, Writes, Reads, JsPath}
import play.api.libs.functional.syntax._

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
      "destination"       -> trip.destination,
      "outbound"          -> Json.toJson(trip.outbound),
      "inbound"           -> Json.toJson(trip.inbound),
      "timeAtDestination" -> Json.toJson(trip.timeAtDestination),
      "totalPrice"        -> Json.toJson(trip.totalPrice),
      "pricePerHour"      -> Json.toJson(trip.pricePerHour)
    )
  }

  implicit val tripReads: Reads[Trip] = (
    (JsPath \ "destination").read[String] and
      (JsPath \ "outbound").read[Flight] and
      (JsPath \ "inbound").read[Flight]
  )(Trip.apply _)
}
