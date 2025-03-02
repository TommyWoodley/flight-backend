package model

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, Reads, Writes}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class Flight(
    flightNumber: String,
    airline: String,
    departureCode: String,
    arrivalCode: String,
    departureTime: LocalDateTime,
    arrivalTime: LocalDateTime,
    price: Double = 0.0
)

object Flight {
  implicit val flightWrites: Writes[Flight]             = Json.writes[Flight]
  implicit val localDateTimeReads: Reads[LocalDateTime] = Reads[LocalDateTime](js =>
    js.validate[String]
      .map[LocalDateTime](dtString => LocalDateTime.parse(dtString, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  )

  implicit val flightReads: Reads[Flight] = (
    (JsPath \ "flightNumber").read[String] and
      (JsPath \ "marketingCarrier" \ "name").read[String] and
      (JsPath \ "origin" \ "displayCode").read[String] and
      (JsPath \ "destination" \ "displayCode").read[String] and
      (JsPath \ "departure").read[LocalDateTime] and
      (JsPath \ "arrival").read[LocalDateTime] and
      Reads.pure(0.0)
  )(Flight.apply _)
}
