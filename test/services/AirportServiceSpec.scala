package services

import model.Airport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class AirportServiceSpec extends AnyFlatSpec with Matchers {

  "Airport Service" should "fetch and parse airports correctly" in {

    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.allAirports must contain theSameElementsAs List(
      Airport("LCY", "London City Airport", "LCY", "95565047"),
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051"),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050"),
      Airport("STN", "London Stansted Airport", "STN", "95565052"),
      Airport("LTN", "Luton Airport", "LTN", "95565053"),
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041"),
      Airport("ORY", "Paris Orly", "ORY", "95565040"))
  }

  "Airport Service" should "return SkyId and EntityId for a given iata code" in {
    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.getAirport("LCY") must be(Some(("LCY", "95565047")))
    airportCache.getAirport("LGW") must be(Some(("LGW", "95565051")))
    airportCache.getAirport("LHR") must be(Some(("LHR", "95565050")))
    airportCache.getAirport("STN") must be(Some(("STN", "95565052")))
    airportCache.getAirport("LTN") must be(Some(("LTN", "95565053")))
  }

}
