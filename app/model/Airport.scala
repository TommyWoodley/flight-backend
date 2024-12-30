package model

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads, Writes}

case class Airport(code: String, name: String, skyId: String, entity: String, country: String)

object Airport {
  implicit val airportWrites: Writes[Airport] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "name").write[String]
    )(airport => (airport.code, airport.name))

  implicit val reads: Reads[Airport] = (
    (JsPath \ "iata_code").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "skyId").read[String] and
      (JsPath \ "entityId").read[String] and
      (JsPath \ "country").read[String]
    )(Airport.apply _)
}
