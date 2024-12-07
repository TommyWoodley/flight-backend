package cache

import model.Airport
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsValue, Json}
import services.ApiService

import scala.io.Source

class AirportCacheSpec extends AnyFlatSpec with Matchers {

  "Airport Cache" should "fetch and parse airports correctly" in {
    val mockApiService = mock[ApiService]

    // load json file from httpRequests/flightLabs/getCitiesLON.json
    val source = Source.fromFile("httpRequests/flightLabs/getCitiesLON.json")
    val jsonString = try source.mkString finally source.close()
    val jsonResponse: JsValue = Json.parse(jsonString)

    // Set up the mock to return the sample JSON response
    when(mockApiService.fetchAirportsForCity("LON")).thenReturn(jsonResponse)

    // Create an instance of AirportCache with the mocked ApiService
    val airportCache = new AirportCache(mockApiService)

    airportCache.allAirports must contain theSameElementsAs List(
      Airport("LCY", "London City Airport"),
      Airport("LGW", "London Gatwick Airport"),
      Airport("LHR", "London Heathrow Airport"),
      Airport("STN", "London Stansted Airport"),
      Airport("LTN", "Luton Airport"))
  }

}
