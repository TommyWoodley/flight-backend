package model

import java.time.ZonedDateTime

case class Flight(flightNumber: String, airline: String, departure: Airport, arrival: Airport,
                  departureTime: ZonedDateTime, arrivalTime: ZonedDateTime)
