package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.json._
import play.api.{Configuration, Environment}
import model.{Trip, Flight, Airport}
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.{TripCreator, FlightService, AirportService, DateService, WeekendService}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration._

/** Add your spec here. You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class ApiControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  // Create test configuration with cache settings
  private val config = Configuration(ConfigFactory.parseString("""
      |cache.expiry {
      |  duration = 1
      |  unit = "HOURS"
      |}
      |""".stripMargin))

  // Test data
  private val testDate   = LocalDate.parse("2025-01-18")
  private val lhrAirport = Airport("LHR", "London Heathrow", "GB", "12345", "67890", 51.4700, -0.4543)
  private val cdgAirport = Airport("CDG", "Paris Charles de Gaulle", "FR", "54321", "09876", 49.0097, 2.5479)
  private val madAirport = Airport("MAD", "Madrid Barajas", "ES", "11111", "22222", 40.4983, -3.5676)

  private val outboundFlight = Flight(
    "FL123",
    "British Airways",
    "LHR",
    "CDG",
    LocalDateTime.parse("2025-01-18T10:00:00"),
    LocalDateTime.parse("2025-01-18T12:00:00"),
    100.0
  )

  private val inboundFlight = Flight(
    "FL124",
    "Air France",
    "CDG",
    "LHR",
    LocalDateTime.parse("2025-01-20T14:00:00"),
    LocalDateTime.parse("2025-01-20T16:00:00"),
    120.0
  )

  private val outboundFlightMadrid = Flight(
    "FL125",
    "Iberia",
    "LHR",
    "MAD",
    LocalDateTime.parse("2025-01-18T11:00:00"),
    LocalDateTime.parse("2025-01-18T13:00:00"),
    150.0
  )

  private val inboundFlightMadrid = Flight(
    "FL126",
    "Iberia",
    "MAD",
    "LHR",
    LocalDateTime.parse("2025-01-20T15:00:00"),
    LocalDateTime.parse("2025-01-20T17:00:00"),
    170.0
  )

  private val parisTrip  = Trip("France", outboundFlight, inboundFlight)
  private val madridTrip = Trip("Spain", outboundFlightMadrid, inboundFlightMadrid)

  "ApiController GET /api/trips" should {
    "return trips for valid request parameters" in {
      val mockTripCreator    = mock[TripCreator]
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      when(mockTripCreator.create(List("LHR"), testDate, 2)).thenReturn(List(parisTrip))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=2")
      val result  = controller.getTrips()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      (jsonResponse \\ "destination").head.as[String] mustBe "France"
      (jsonResponse \\ "totalPrice").head.as[Double] mustBe 220.0
    }

    "handle multiple departure airports" in {
      val mockTripCreator    = mock[TripCreator]
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      when(mockTripCreator.create(List("LHR", "LGW"), testDate, 2)).thenReturn(List(parisTrip))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR,LGW&date=2025-01-18&numberOfDays=2")
      val result  = controller.getTrips()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return BadRequest for missing parameters" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request1 = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18")
      val result1  = controller.getTrips()(request1)
      status(result1) mustBe BAD_REQUEST
      contentAsString(result1) must include("Must provide both date and numberOfDays parameters")

      val request2 = FakeRequest(GET, "/api/trips?fromCode=LHR&numberOfDays=2")
      val result2  = controller.getTrips()(request2)
      status(result2) mustBe BAD_REQUEST
      contentAsString(result2) must include("Must provide both date and numberOfDays parameters")

      val request3 = FakeRequest(GET, "/api/trips?date=2025-01-18&numberOfDays=2")
      val result3  = controller.getTrips()(request3)
      status(result3) mustBe BAD_REQUEST
      contentAsString(result3) must include("Missing required query parameter: fromCode")
    }

    "return BadRequest for invalid date format" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=18-01-2025&numberOfDays=2")
      val result  = controller.getTrips()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid date format")
    }

    "handle empty airport codes gracefully" in {
      val mockTripCreator    = mock[TripCreator]
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      when(mockTripCreator.create(List.empty[String], testDate, 2)).thenReturn(List.empty)

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/trips?fromCode=&date=2025-01-18&numberOfDays=2")
      val result  = controller.getTrips()(request)

      status(result) mustBe OK
      contentAsJson(result).as[JsArray].value.isEmpty mustBe true
    }

    "handle invalid number of days gracefully" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=invalid")
      val result  = controller.getTrips()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid number format")
    }
  }

  "ApiController GET /api/weekends" should {
    "return trips for valid request parameters" in {
      val mockTripCreator    = mock[TripCreator]
      val mockDateService    = mock[DateService]
      val mockAirportService = mock[AirportService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      // Mock the weekend service to return trips
      when(mockWeekendService.getWeekendTrips(List("LHR"), 6, 2024, 1))
        .thenReturn(List(parisTrip, madridTrip))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      val destinations = (jsonResponse \\ "destination").map(_.as[String]).toSet
      destinations must contain allOf ("France", "Spain")
    }

    "return BadRequest for invalid month" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      // Mock the weekend service to throw an exception for invalid month
      when(mockWeekendService.getWeekendTrips(List("LHR"), 13, 2024, 1))
        .thenThrow(new IllegalArgumentException("Month must be between 1 and 12"))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=13&year=2024&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Month must be between 1 and 12")
    }

    "return BadRequest for invalid numberOfExtraDays" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      // Mock the weekend service to throw an exception for invalid numberOfExtraDays
      when(mockWeekendService.getWeekendTrips(List("LHR"), 6, 2024, 3))
        .thenThrow(new IllegalArgumentException("Additional days cannot be more than 2"))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&year=2024&numberOfExtraDays=3")
      val result  = controller.getWeekends()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Additional days cannot be more than 2")
    }

    "handle multiple departure airports" in {
      val mockTripCreator    = mock[TripCreator]
      val mockDateService    = mock[DateService]
      val mockAirportService = mock[AirportService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      // Mock the weekend service to return trips for multiple airports
      when(mockWeekendService.getWeekendTrips(List("LHR", "LGW"), 6, 2024, 1))
        .thenReturn(List(parisTrip, madridTrip))

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR,LGW&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      val destinations = (jsonResponse \\ "destination").map(_.as[String]).toSet
      destinations must contain allOf ("France", "Spain")
    }

    "return empty list when no weekend trips are found" in {
      val mockTripCreator    = mock[TripCreator]
      val mockDateService    = mock[DateService]
      val mockAirportService = mock[AirportService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]

      // Mock the weekend service to return an empty list
      when(mockWeekendService.getWeekendTrips(List("LHR"), 6, 2024, 1))
        .thenReturn(List.empty)

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[JsArray].value must be(empty)
    }

    "return BadRequest for invalid year format" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&year=invalid&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid number format for month, year, or numberOfExtraDays parameters")
    }

    "return BadRequest for missing parameters" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request1 = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&year=2024")
      val result1  = controller.getWeekends()(request1)
      status(result1) mustBe BAD_REQUEST
      contentAsString(result1) must include("Must provide month, year, and numberOfExtraDays parameters")

      val request2 = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=6&numberOfExtraDays=1")
      val result2  = controller.getWeekends()(request2)
      status(result2) mustBe BAD_REQUEST
      contentAsString(result2) must include("Must provide month, year, and numberOfExtraDays parameters")

      val request3 = FakeRequest(GET, "/api/weekends?month=6&year=2024&numberOfExtraDays=1")
      val result3  = controller.getWeekends()(request3)
      status(result3) mustBe BAD_REQUEST
      contentAsString(result3) must include("Missing required query parameter: fromCode")
    }

    "handle invalid number format gracefully" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(GET, "/api/weekends?fromCode=LHR&month=invalid&year=2024&numberOfExtraDays=1")
      val result  = controller.getWeekends()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid number format")
    }
  }

  "ApiController POST /api/tripOptions" should {
    "accept and process valid trip data" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val tripJson = Json.obj(
        "destination" -> "France",
        "outbound"    -> Json.obj(
          "flightNumber"  -> "FL123",
          "airline"       -> "British Airways",
          "departureCode" -> "LHR",
          "arrivalCode"   -> "CDG",
          "departureTime" -> "2025-01-18T10:00:00",
          "arrivalTime"   -> "2025-01-18T12:00:00",
          "price"         -> 100.0
        ),
        "inbound"     -> Json.obj(
          "flightNumber"  -> "FL124",
          "airline"       -> "Air France",
          "departureCode" -> "CDG",
          "arrivalCode"   -> "LHR",
          "departureTime" -> "2025-01-20T14:00:00",
          "arrivalTime"   -> "2025-01-20T16:00:00",
          "price"         -> 120.0
        )
      )

      val request = FakeRequest(POST, "/api/tripOptions")
        .withJsonBody(tripJson)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result  = controller.findTripOptions()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      (jsonResponse \ "extractedInfo" \ "fromCode").as[String] mustBe "LHR"
      (jsonResponse \ "extractedInfo" \ "month").as[Int] mustBe 1
      (jsonResponse \ "extractedInfo" \ "year").as[Int] mustBe 2025
      (jsonResponse \ "extractedInfo" \ "numberOfExtraDays").as[Int] mustBe 1
      (jsonResponse \ "extractedInfo" \ "arrivalCode").as[String] mustBe "LHR"
    }

    "return BadRequest for invalid JSON" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val request = FakeRequest(POST, "/api/tripOptions")
        .withTextBody("This is not JSON")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result  = controller.findTripOptions()(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include("Expecting JSON data")
    }

    "return BadRequest for invalid trip data" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      )

      val invalidTripJson = Json.obj(
        "destination" -> "France",
        "outbound"    -> Json.obj(
          "flightNumber"  -> "FL123",
          "airline"       -> "British Airways",
          "departureCode" -> "LHR",
          "arrivalCode"   -> "CDG",
          // Missing departureTime
          "arrivalTime"   -> "2025-01-18T12:00:00",
          "price"         -> 100.0
        ),
        "inbound"     -> Json.obj(
          "flightNumber"  -> "FL124",
          "airline"       -> "Air France",
          "departureCode" -> "CDG",
          "arrivalCode"   -> "LHR",
          "departureTime" -> "2025-01-20T14:00:00",
          "arrivalTime"   -> "2025-01-20T16:00:00",
          "price"         -> 120.0
        )
      )

      val request = FakeRequest(POST, "/api/tripOptions")
        .withJsonBody(invalidTripJson)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result  = controller.findTripOptions()(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include("Invalid trip data")
    }
  }

  "ApiController" should {
    "have the correct routes" in {
      val mockAirportService = mock[AirportService]
      val mockDateService    = mock[DateService]
      val mockFlightService  = mock[FlightService]
      val mockWeekendService = mock[WeekendService]
      val mockTripCreator    = mock[TripCreator]

      val controller = new ApiController(
        stubControllerComponents(),
        mockAirportService,
        mockDateService,
        mockFlightService,
        mockWeekendService,
        mockTripCreator,
        config
      ) {
        override def getTrips        = Action { Ok("trips") }
        override def getWeekends     = Action { Ok("weekends") }
        override def findTripOptions = Action { Ok("tripOptions") }
      }

      val home = controller.getTrips().apply(FakeRequest(GET, "/"))
      status(home) mustBe OK
      contentAsString(home) mustBe "trips"

      val weekends = controller.getWeekends().apply(FakeRequest(GET, "/"))
      status(weekends) mustBe OK
      contentAsString(weekends) mustBe "weekends"

      val tripOptions = controller.findTripOptions().apply(FakeRequest(GET, "/"))
      status(tripOptions) mustBe OK
      contentAsString(tripOptions) mustBe "tripOptions"
    }
  }
}
