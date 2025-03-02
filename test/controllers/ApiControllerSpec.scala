package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.json._
import play.api.{Configuration, Environment}
import model.{Trip, Flight, Airport}
import com.typesafe.config.ConfigFactory
import org.mockito.MockitoSugar
import services.{TripCreator, FlightService, AirportService}

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

  private val testTrip = Trip("CDG", outboundFlight, inboundFlight)

  "ApiController" should {

    "return trips for valid request parameters" in {
      // Setup mocks
      val mockTripCreator = mock[TripCreator]
      when(mockTripCreator.create(List("LHR"), testDate, 2)).thenReturn(List(testTrip))

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=2")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      (jsonResponse \\ "destination").nonEmpty mustBe true
    }

    "handle multiple departure airports" in {
      // Setup mocks
      val mockTripCreator = mock[TripCreator]
      when(mockTripCreator.create(List("LHR", "LGW"), testDate, 2)).thenReturn(List(testTrip))

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR,LGW&date=2025-01-18&numberOfDays=2")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return BadRequest for missing parameters" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request1 = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18")
      val result1  = controller.getApiData()(request1)
      status(result1) mustBe BAD_REQUEST
      contentAsString(result1) must include("Missing required query parameters")

      val request2 = FakeRequest(GET, "/api/trips?fromCode=LHR&numberOfDays=2")
      val result2  = controller.getApiData()(request2)
      status(result2) mustBe BAD_REQUEST
      contentAsString(result2) must include("Missing required query parameters")

      val request3 = FakeRequest(GET, "/api/trips?date=2025-01-18&numberOfDays=2")
      val result3  = controller.getApiData()(request3)
      status(result3) mustBe BAD_REQUEST
      contentAsString(result3) must include("Missing required query parameters")
    }

    "return BadRequest for invalid date format" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=18-01-2025&numberOfDays=2")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid date format")
    }

    "handle empty airport codes gracefully" in {
      val mockTripCreator = mock[TripCreator]
      when(mockTripCreator.create(List.empty[String], testDate, 2)).thenReturn(List.empty)

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=&date=2025-01-18&numberOfDays=2")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentAsJson(result).as[JsArray].value.isEmpty mustBe true
    }

    "handle invalid number of days gracefully" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=invalid")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid number format")
    }
  }
}
