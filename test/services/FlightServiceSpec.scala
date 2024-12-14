package services

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

import scala.io.Source

class FlightServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "FlightService" should "call the correct endpoint to retrieve flights" in {
    // Arrange
    val mockApiService = mock[ApiService]
    val flightService = new FlightService(mockApiService, new AirportService)
    val fromCode = "LGW"
    val toCode = "CDG"
    val endpoint = "/retrieveFlights"
    val params = Map(
      "originSkyId" -> "LGW",
      "destinationSkyId" -> "CDG",
      "originEntityId" -> "95565051",
      "destinationEntityId" -> "95565041",
      "date" -> "2024-12-13"
    )

    // Load the completeFlightsJson from the file
    val source = Source.fromFile("httpRequests/flightLabs/completeFlights.json")
    val jsonString = try source.mkString finally source.close()
    val json = Json.parse(jsonString)

    // Configure the mock to return the completeFlightsJson
    when(mockApiService.get(endpoint, params)).thenReturn(json)

    // Act
    val result = flightService.getFlights(fromCode, toCode)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result should have size 4
  }
}
