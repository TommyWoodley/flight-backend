package model

import play.api.libs.json.{Json, Writes}

case class Airport(code: String, name: String)

object Airport {
  implicit val airportWrites: Writes[Airport] = Json.writes[Airport]
}