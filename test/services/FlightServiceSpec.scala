package services

import model.Airport
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

import java.time.LocalDate
import scala.io.Source
import scala.util.Success

class FlightServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar {
  val from: Airport = Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821)
  val to: Airport   = Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479)

  "FlightService" should "call the correct endpoint to retrieve flights" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val date                = LocalDate.of(2024, 12, 13)
    val endpoint            = "/retrieveFlights"
    val params              = Map(
      "originSkyId"         -> "LGW",
      "destinationSkyId"    -> "CDG",
      "originEntityId"      -> "95565051",
      "destinationEntityId" -> "95565041",
      "date"                -> "2024-12-13",
      "currency"            -> "GBP"
    )

    // Load the completeFlightsJson from the file
    val source     = Source.fromFile("httpRequests/flightLabs/completeFlights.json")
    val jsonString =
      try source.mkString
      finally source.close()
    val json       = Json.parse(jsonString)

    // Configure the mock to return the completeFlightsJson
    when(mockApiService.get(endpoint, params)).thenReturn(Some(json))
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(None))
    when(mockDynamoDBService.storeFlights(any(), any(), any(), any())).thenReturn(Success(()))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result should have size 4
  }

  it should "handle incomplete flight data and call the incomplete flights endpoint" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val endpoint            = "/retrieveFlights"
    val incompleteEndpoint  = "/retrieveFlightsIncomplete"
    val date                = LocalDate.of(2024, 12, 13)
    val params              = Map(
      "originSkyId"         -> "LGW",
      "destinationSkyId"    -> "CDG",
      "originEntityId"      -> "95565051",
      "destinationEntityId" -> "95565041",
      "date"                -> "2024-12-13",
      "currency"            -> "GBP"
    )
    val sessionId           =
      "Cl0IARJZCk4KJGM2YTE1NWU2LTFlYzMtNDk1Mi1iNGE0LWZjMGQ1Y2Y4MDYxZRACGiRlNzM1NzQwZS0yYjQ1LTRkNGUtYWY2NS1hOTQxNzRlZjI1OTgQh_nLuboyGAESKHVzc19lMjViN2QwZi1mYjJjLTQxODEtYTAzZC00YmYxMzYyOTk5NGU="
    val incompleteParams    = Map("sessionId" -> sessionId)

    // Load the incompleteFlightsJson from the file
    val sourceIncomplete     = Source.fromFile("httpRequests/flightLabs/incompleteFlights.json")
    val jsonStringIncomplete =
      try sourceIncomplete.mkString
      finally sourceIncomplete.close()
    val jsonIncomplete       = Json.parse(jsonStringIncomplete)

    val sourceComplete     = Source.fromFile("httpRequests/flightLabs/completeFlights.json")
    val jsonStringComplete =
      try sourceComplete.mkString
      finally sourceComplete.close()
    val jsonComplete       = Json.parse(jsonStringComplete)

    // Configure the mock to return the incompleteFlightsJson
    when(mockApiService.get(endpoint, params)).thenReturn(Some(jsonIncomplete))
    when(mockApiService.get(incompleteEndpoint, incompleteParams)).thenReturn(Some(jsonComplete))
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(None))
    when(mockDynamoDBService.storeFlights(any(), any(), any(), any())).thenReturn(Success(()))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    verify(mockApiService).get(incompleteEndpoint, incompleteParams)
    result should have size 8
  }

  it should "retrieve flights with non-zero prices" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val date                = LocalDate.of(2024, 12, 13)
    val endpoint            = "/retrieveFlights"
    val params              = Map(
      "originSkyId"         -> "LGW",
      "destinationSkyId"    -> "CDG",
      "originEntityId"      -> "95565051",
      "destinationEntityId" -> "95565041",
      "date"                -> "2024-12-13",
      "currency"            -> "GBP"
    )

    // Load the completeFlightsJson from the file
    val source     = Source.fromFile("httpRequests/flightLabs/completeFlights.json")
    val jsonString =
      try source.mkString
      finally source.close()
    val json       = Json.parse(jsonString)

    // Configure the mock to return the completeFlightsJson
    when(mockApiService.get(endpoint, params)).thenReturn(Some(json))
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(None))
    when(mockDynamoDBService.storeFlights(any(), any(), any(), any())).thenReturn(Success(()))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result should have size 4
    all(result.map(_.price)) should not be 0.0
  }

  it should "handle the scenario where the first call to the endpoint fails" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val date                = LocalDate.of(2024, 12, 13)
    val endpoint            = "/retrieveFlights"
    val params              = Map(
      "originSkyId"         -> "LGW",
      "destinationSkyId"    -> "CDG",
      "originEntityId"      -> "95565051",
      "destinationEntityId" -> "95565041",
      "date"                -> "2024-12-13",
      "currency"            -> "GBP"
    )

    // Configure the mock to return None
    when(mockApiService.get(endpoint, params)).thenReturn(None)
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(None))
    when(mockDynamoDBService.storeFlights(any(), any(), any(), any())).thenReturn(Success(()))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    result shouldBe empty
  }

  it should "handle the scenario where the first call succeeds and the second call fails" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val endpoint            = "/retrieveFlights"
    val incompleteEndpoint  = "/retrieveFlightsIncomplete"
    val date                = LocalDate.of(2024, 12, 13)
    val params              = Map(
      "originSkyId"         -> "LGW",
      "destinationSkyId"    -> "CDG",
      "originEntityId"      -> "95565051",
      "destinationEntityId" -> "95565041",
      "date"                -> "2024-12-13",
      "currency"            -> "GBP"
    )
    val sessionId           =
      "Cl0IARJZCk4KJGM2YTE1NWU2LTFlYzMtNDk1Mi1iNGE0LWZjMGQ1Y2Y4MDYxZRACGiRlNzM1NzQwZS0yYjQ1LTRkNGUtYWY2NS1hOTQxNzRlZjI1OTgQh_nLuboyGAESKHVzc19lMjViN2QwZi1mYjJjLTQxODEtYTAzZC00YmYxMzYyOTk5NGU="
    val incompleteParams    = Map("sessionId" -> sessionId)

    // Load the incompleteFlightsJson from the file
    val sourceIncomplete     = Source.fromFile("httpRequests/flightLabs/incompleteFlights.json")
    val jsonStringIncomplete =
      try sourceIncomplete.mkString
      finally sourceIncomplete.close()
    val jsonIncomplete       = Json.parse(jsonStringIncomplete)

    // Configure the mock to return the incompleteFlightsJson and then None
    when(mockApiService.get(endpoint, params)).thenReturn(Some(jsonIncomplete))
    when(mockApiService.get(incompleteEndpoint, incompleteParams)).thenReturn(None)
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(None))
    when(mockDynamoDBService.storeFlights(any(), any(), any(), any())).thenReturn(Success(()))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockApiService).get(endpoint, params)
    verify(mockApiService).get(incompleteEndpoint, incompleteParams)
    result should have size 4
  }

  it should "retrieve flights from database if available" in {
    // Arrange
    val mockApiService      = mock[ApiService]
    val mockDynamoDBService = mock[DynamoDBService]
    val flightService       = new FlightService(mockApiService, mockDynamoDBService)
    val date                = LocalDate.of(2024, 12, 13)

    // Mock flights to return from database
    val mockFlights = List(
      model.Flight(
        "FL123",
        "British Airways",
        "LGW",
        "CDG",
        java.time.LocalDateTime.of(2024, 12, 13, 10, 0),
        java.time.LocalDateTime.of(2024, 12, 13, 12, 0),
        100.0
      )
    )

    // Configure the mock to return flights from database
    when(mockDynamoDBService.getFlights(from, to, date)).thenReturn(Success(Some(mockFlights)))

    // Act
    val result = flightService.getFlights(from, to, date)

    // Assert
    verify(mockDynamoDBService).getFlights(from, to, date)
    // Verify that API service was not called
    verifyNoInteractions(mockApiService)
    result should have size 1
  }
}
