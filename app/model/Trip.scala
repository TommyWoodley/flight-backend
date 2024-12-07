package model

import play.api.libs.json.{Json, Writes}

import java.time.Duration

case class Trip(destination: String, outbound: Flight, inbound: Flight) {
  def timeAtDestination: Long = {
    Duration.between(outbound.arrivalTime, inbound.departureTime).toHours
  }
}

object Trip {
  implicit val tripWrites: Writes[Trip] = (trip: Trip) => {
    val baseJson = Json.obj(
      "destination" -> trip.destination,
      "outbound" -> Json.toJson(trip.outbound),
      "inbound" -> Json.toJson(trip.inbound)
    )
    baseJson + ("timeAtDestination" -> Json.toJson(trip.timeAtDestination))
  }
}