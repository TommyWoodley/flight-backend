package model

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads, Writes}

case class Airport(
    code: String,
    name: String,
    skyId: String,
    entity: String,
    country: String,
    latitude: Double,
    longitude: Double
)

object Airport {
  implicit val airportWrites: Writes[Airport] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "skyId").write[String] and
      (JsPath \ "entity").write[String] and
      (JsPath \ "country").write[String] and
      (JsPath \ "latitude").write[Double] and
      (JsPath \ "longitude").write[Double]
  )(airport =>
    (airport.code, airport.name, airport.skyId, airport.entity, airport.country, airport.latitude, airport.longitude)
  )

  implicit val reads: Reads[Airport] = (
    (JsPath \ "iata_code").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "skyId").read[String] and
      (JsPath \ "entityId").read[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "latitude").read[Double] and
      (JsPath \ "longitude").read[Double]
  )(Airport.apply _)
}
