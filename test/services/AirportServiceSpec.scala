package services

import model.Airport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class AirportServiceSpec extends AnyFlatSpec with Matchers {

  "Airport Service" should "fetch and parse airports correctly" in {

    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.allAirports must contain allOf (Airport(
      "LCY",
      "London City Airport",
      "LCY",
      "95565047",
      "United Kingdom"
    ),
    Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom"),
    Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom"),
    Airport("STN", "London Stansted Airport", "STN", "95565052", "United Kingdom"),
    Airport("LTN", "Luton Airport", "LTN", "95565053", "United Kingdom"),
    Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France"),
    Airport("ORY", "Paris Orly", "ORY", "95565040", "France"))
  }

  it should "return SkyId and EntityId for a given iata code" in {
    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.getAirport("LCY") must be(Some(("LCY", "95565047")))
    airportCache.getAirport("LGW") must be(Some(("LGW", "95565051")))
    airportCache.getAirport("LHR") must be(Some(("LHR", "95565050")))
    airportCache.getAirport("STN") must be(Some(("STN", "95565052")))
    airportCache.getAirport("LTN") must be(Some(("LTN", "95565053")))
  }

  it should "return the full Airport object for a given code" in {
    val airportService = new AirportService()

    airportService.getAirportByCode("LCY") must be(
      Airport("LCY", "London City Airport", "LCY", "95565047", "United Kingdom")
    )
    airportService.getAirportByCode("LGW") must be(
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom")
    )
    airportService.getAirportByCode("LHR") must be(
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom")
    )
    airportService.getAirportByCode("STN") must be(
      Airport("STN", "London Stansted Airport", "STN", "95565052", "United Kingdom")
    )
    airportService.getAirportByCode("LTN") must be(Airport("LTN", "Luton Airport", "LTN", "95565053", "United Kingdom"))
  }

  it should "return all airports in a different country" in {
    val airportService = new AirportService()

    val result = airportService.getAllAirportsInADifferentCountry("United Kingdom")
    result must contain allOf (
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France"),
      Airport("ORY", "Paris Orly", "ORY", "95565040", "France")
    )
  }

  it should "return a list of Airport objects for given IATA codes" in {
    val airportService = new AirportService()

    val result = airportService.getAirportsByCode(List("LCY", "LGW", "LHR"))
    result must contain allOf (
      Airport("LCY", "London City Airport", "LCY", "95565047", "United Kingdom"),
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom"),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom")
    )
  }

}
