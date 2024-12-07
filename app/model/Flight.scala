package model

import play.api.libs.json.{Json, Writes}

import java.time.ZonedDateTime

case class Flight(flightNumber: String, airline: String, departure: Airport, arrival: Airport,
                  departureTime: ZonedDateTime, arrivalTime: ZonedDateTime)

object Flight {
  implicit val flightWrites: Writes[Flight] = Json.writes[Flight]
}