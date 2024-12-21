package services

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

import java.time.LocalDate
import scala.io.Source

class FlightServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "FlightService" should "call the correct endpoint to retrieve flights" in {
    // Arrange
    val mockApiService = mock[ApiService]
    val flightService = new FlightService(mockApiService, new AirportService)
    val fromCode = "LGW"
    val toCode = "CDG"
    val date = LocalDate.of(2024, 12, 13)
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
    val result = flightService.getFlights(fromCode, toCode, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result should have size 4
  }

  it should "handle incomplete flight data and call the incomplete flights endpoint" in {
    // Arrange
    val mockApiService = mock[ApiService]
    val flightService = new FlightService(mockApiService, new AirportService)
    val fromCode = "LGW"
    val toCode = "CDG"
    val endpoint = "/retrieveFlights"
    val incompleteEndpoint = "/retrieveFlightsIncomplete"
    val date = LocalDate.of(2024, 12, 13)
    val params = Map(
      "originSkyId" -> "LGW",
      "destinationSkyId" -> "CDG",
      "originEntityId" -> "95565051",
      "destinationEntityId" -> "95565041",
      "date" -> "2024-12-13"
    )
    val sessionId = "Cl0IARJZCk4KJGM2YTE1NWU2LTFlYzMtNDk1Mi1iNGE0LWZjMGQ1Y2Y4MDYxZRACGiRlNzM1NzQwZS0yYjQ1LTRkNGUtYWY2NS1hOTQxNzRlZjI1OTgQh_nLuboyGAESKHVzc19lMjViN2QwZi1mYjJjLTQxODEtYTAzZC00YmYxMzYyOTk5NGU="
    val incompleteParams = Map("sessionId" -> sessionId)

    // Load the incompleteFlightsJson from the file
    val sourceIncomplete = Source.fromFile("httpRequests/flightLabs/incompleteFlights.json")
    val jsonStringIncomplete = try sourceIncomplete.mkString finally sourceIncomplete.close()
    val jsonIncomplete = Json.parse(jsonStringIncomplete)

    val sourceComplete = Source.fromFile("httpRequests/flightLabs/completeFlights.json")
    val jsonStringComplete = try sourceComplete.mkString finally sourceComplete.close()
    val jsonComplete = Json.parse(jsonStringComplete)

    // Configure the mock to return the incompleteFlightsJson
    when(mockApiService.get(endpoint, params)).thenReturn(jsonIncomplete)
    when(mockApiService.get(incompleteEndpoint, incompleteParams)).thenReturn(jsonComplete)

    // Act
    val result = flightService.getFlights(fromCode, toCode, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    verify(mockApiService).get(incompleteEndpoint, incompleteParams)
    result should have size 8
  }

  it should "retrieve flights with non-zero prices" in {
    // Arrange
    val mockApiService = mock[ApiService]
    val flightService = new FlightService(mockApiService, new AirportService)
    val fromCode = "LGW"
    val toCode = "CDG"
    val date = LocalDate.of(2024, 12, 13)
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
    val result = flightService.getFlights(fromCode, toCode, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result should have size 4
    all(result.map(_.price)) should not be 0.0
  }
}
