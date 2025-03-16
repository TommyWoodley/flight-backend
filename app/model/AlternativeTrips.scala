package model

import play.api.libs.json.{Json, Writes}

case class AlternativeTrips(
    selected_trip: Trip,
    alternative_weekends: List[Trip]
)

object AlternativeTrips {
  implicit val alternativeTripsWrites: Writes[AlternativeTrips] = Json.writes[AlternativeTrips]
}
