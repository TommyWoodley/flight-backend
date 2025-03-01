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
      "United Kingdom",
      51.5048,
      0.0495
    ),
    Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821),
    Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543),
    Airport("STN", "London Stansted Airport", "STN", "95565052", "United Kingdom", 51.8860, 0.2389),
    Airport("LTN", "Luton Airport", "LTN", "95565053", "United Kingdom", 51.8747, -0.3683),
    Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479),
    Airport("ORY", "Paris Orly", "ORY", "95565040", "France", 48.7262, 2.3652))
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
      Airport("LCY", "London City Airport", "LCY", "95565047", "United Kingdom", 51.5048, 0.0495)
    )
    airportService.getAirportByCode("LGW") must be(
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821)
    )
    airportService.getAirportByCode("LHR") must be(
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543)
    )
    airportService.getAirportByCode("STN") must be(
      Airport("STN", "London Stansted Airport", "STN", "95565052", "United Kingdom", 51.8860, 0.2389)
    )
    airportService.getAirportByCode("LTN") must be(
      Airport("LTN", "Luton Airport", "LTN", "95565053", "United Kingdom", 51.8747, -0.3683)
    )
  }

  it should "return all airports in a different country" in {
    val airportService = new AirportService()

    val result = airportService.getAllAirportsInADifferentCountry("United Kingdom")
    result must contain allOf (
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479),
      Airport("ORY", "Paris Orly", "ORY", "95565040", "France", 48.7262, 2.3652)
    )
  }

  it should "return a list of Airport objects for given IATA codes" in {
    val airportService = new AirportService()

    val result = airportService.getAirportsByCode(List("LCY", "LGW", "LHR"))
    result must contain allOf (
      Airport("LCY", "London City Airport", "LCY", "95565047", "United Kingdom", 51.5048, 0.0495),
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543)
    )
  }

}
