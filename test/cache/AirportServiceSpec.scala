package cache

import model.Airport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import services.AirportService

class AirportServiceSpec extends AnyFlatSpec with Matchers {

  "Airport Cache" should "fetch and parse airports correctly" in {

    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportService()

    airportCache.allAirports must contain theSameElementsAs List(
      Airport("LCY", "London City Airport", "LCY", "95565047"),
      Airport("LGW", "London Gatwick Airport", "LGW", "95565051"),
      Airport("LHR", "London Heathrow Airport", "LHR", "95565050"),
      Airport("STN", "London Stansted Airport", "STN", "95565052"),
      Airport("LTN", "Luton Airport", "LTN", "95565053"))
  }

}
