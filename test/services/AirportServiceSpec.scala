package services

import model.Airport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class AirportServiceSpec extends AnyFlatSpec with Matchers {

  "Airport Service" should "fetch and parse airports correctly" in {

    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.allAirports must contain allOf (
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543),
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479),
      Airport("ORY", "Paris Orly", "ORY", "95565040", "France", 48.7262, 2.3652),
      Airport("AMS", "Amsterdam Schiphol", "AMS", "95565044", "Netherlands", 52.3086, 4.7639),
      Airport("ATH", "Athens International", "ATH", "95673624", "Greece", 37.9364, 23.9445),
      Airport("BEG", "Belgrade Nikola Tesla", "BEG", "95673488", "Serbia", 44.8184, 20.309),
      Airport("BER", "Berlin Brandenburg", "BER", "95673383", "Germany", 52.3667, 13.5033)
    )
  }

  it should "return SkyId and EntityId for a given iata code" in {
    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.getAirport("LGW") must be(Some(("LGW", "95565051")))
    airportCache.getAirport("LHR") must be(Some(("LHR", "95565050")))
    airportCache.getAirport("CDG") must be(Some(("CDG", "95565041")))
  }

  it should "return the full Airport object for a given code" in {
    val airportService = new AirportService()

    airportService.getAirportByCode("LGW") must be(
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821)
    )
    airportService.getAirportByCode("LHR") must be(
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543)
    )
  }

  it should "return all airports in a different country" in {
    val airportService = new AirportService()

    val result = airportService.getAllAirportsInADifferentCountry("United Kingdom")
    result must contain allOf (
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479),
      Airport("ORY", "Paris Orly", "ORY", "95565040", "France", 48.7262, 2.3652),
      Airport("AMS", "Amsterdam Schiphol", "AMS", "95565044", "Netherlands", 52.3086, 4.7639)
    )
  }

  it should "return a list of Airport objects for given IATA codes" in {
    val airportService = new AirportService()

    val result = airportService.getAirportsByCode(List("LGW", "LHR"))
    result must contain allOf (
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050", "United Kingdom", 51.4700, -0.4543)
    )
  }

}
