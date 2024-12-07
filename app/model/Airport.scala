package model

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Airport(code: String, name: String)

object Airport {
  implicit val airportWrites: Writes[Airport] = Json.writes[Airport]

  implicit val reads: Reads[Airport] = (
    (JsPath \ "iata_code").read[String] and
      (JsPath \ "name").read[String]
    )(Airport.apply _)
}