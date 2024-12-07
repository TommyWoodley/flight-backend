package cache

import model.{Airport, Flight}

import java.time.{ZoneId, ZonedDateTime}

class FlightCache {
  private val flights: List[Flight] = List(
    Flight("BA001", "British Airways", Airport("LHR", "London Heathrow"), Airport("CDG", "Charles de Gaulle"), ZonedDateTime.of(2023, 11, 3, 10, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 3, 11, 0, 0, 0, ZoneId.systemDefault())),
    Flight("BA002", "British Airways", Airport("LHR", "London Heathrow"), Airport("CDG", "Charles de Gaulle"), ZonedDateTime.of(2023, 11, 3, 12, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 3, 13, 0, 0, 0, ZoneId.systemDefault())),
    Flight("BA003", "British Airways", Airport("LHR", "London Heathrow"), Airport("CDG", "Charles de Gaulle"), ZonedDateTime.of(2023, 11, 4, 10, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 4, 11, 0, 0, 0, ZoneId.systemDefault())),
    Flight("BA004", "British Airways", Airport("LHR", "London Heathrow"), Airport("CDG", "Charles de Gaulle"), ZonedDateTime.of(2023, 11, 4, 12, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 4, 13, 0, 0, 0, ZoneId.systemDefault())),
    Flight("AF001", "Air France", Airport("CDG", "Charles de Gaulle"), Airport("LHR", "London Heathrow"), ZonedDateTime.of(2023, 11, 5, 10, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 5, 11, 0, 0, 0, ZoneId.systemDefault())),
    Flight("AF002", "Air France", Airport("CDG", "Charles de Gaulle"), Airport("LHR", "London Heathrow"), ZonedDateTime.of(2023, 11, 5, 12, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 5, 13, 0, 0, 0, ZoneId.systemDefault())),
    Flight("AF003", "Air France", Airport("CDG", "Charles de Gaulle"), Airport("LHR", "London Heathrow"), ZonedDateTime.of(2023, 11, 6, 10, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 6, 11, 0, 0, 0, ZoneId.systemDefault())),
    Flight("AF004", "Air France", Airport("CDG", "Charles de Gaulle"), Airport("LHR", "London Heathrow"), ZonedDateTime.of(2023, 11, 6, 12, 0, 0, 0, ZoneId.systemDefault()), ZonedDateTime.of(2023, 11, 6, 13, 0, 0, 0, ZoneId.systemDefault()))
  )

  def getAllFlights: List[Flight] = flights

  def getFlights(fromCode: String, toCode: String): List[Flight] = {
    flights.filter(flight => flight.departure.code == fromCode && flight.arrival.code == toCode)
  }

}
