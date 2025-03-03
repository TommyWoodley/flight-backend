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
import services.{TripCreator, FlightService, AirportService, DateService}

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

  "ApiController" should {
    // Original format tests
    "return trips for valid request parameters using date format" in {
      val mockTripCreator = mock[TripCreator]
      when(mockTripCreator.create(List("LHR"), testDate, 2)).thenReturn(List(parisTrip))

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=2")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      (jsonResponse \\ "destination").head.as[String] mustBe "France"
      (jsonResponse \\ "totalPrice").head.as[Double] mustBe 220.0
    }

    // Weekend format tests
    "return trips for valid request parameters using weekend format" in {
      val mockTripCreator = mock[TripCreator]
      val mockDateService = mock[DateService]

      // Create a real DateService to get access to TripDates
      val realDateService = new DateService
      val dates           = realDateService.getWeekendTrips(6, 2024, 1)

      // Mock the date service to return the same dates
      val mockDates = dates.map { date =>
        mockDateService.TripDates(date.startDate, date.endDate)
      }
      when(mockDateService.getWeekendTrips(eqTo(6), eqTo(2024), eqTo(1))).thenReturn(mockDates)

      // Mock the trip creator for each date
      dates.foreach { date =>
        when(mockTripCreator.create(List("LHR"), date.startDate, 2))
          .thenReturn(List(parisTrip, madridTrip))
      }

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
        override val dateService = mockDateService
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      val destinations = (jsonResponse \\ "destination").map(_.as[String]).toSet
      destinations must contain allOf ("France", "Spain")
    }

    "return BadRequest for invalid month" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&month=13&year=2024&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Month must be between 1 and 12")
    }

    "return BadRequest for invalid numberOfExtraDays" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&month=6&year=2024&numberOfExtraDays=3")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Additional days cannot be more than 2")
    }

    "return BadRequest when mixing date and weekend parameters" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&date=2024-06-15&month=6&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(
        "Must provide either (date and numberOfDays) or (month, year, and numberOfExtraDays)"
      )
    }

    "handle multiple departure airports with weekend format" in {
      val mockTripCreator = mock[TripCreator]
      val mockDateService = mock[DateService]

      // Create a real DateService to get access to TripDates
      val realDateService = new DateService
      val dates           = realDateService.getWeekendTrips(6, 2024, 1)

      // Mock the date service to return the same dates
      val mockDates = dates.map { date =>
        mockDateService.TripDates(date.startDate, date.endDate)
      }
      when(mockDateService.getWeekendTrips(eqTo(6), eqTo(2024), eqTo(1))).thenReturn(mockDates)

      // Mock the trip creator for each date
      dates.foreach { date =>
        when(mockTripCreator.create(List("LHR", "LGW"), date.startDate, 2))
          .thenReturn(List(parisTrip, madridTrip))
      }

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
        override val dateService = mockDateService
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR,LGW&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResponse = contentAsJson(result)
      val destinations = (jsonResponse \\ "destination").map(_.as[String]).toSet
      destinations must contain allOf ("France", "Spain")
    }

    "return empty list when no weekend trips are found" in {
      val mockTripCreator = mock[TripCreator]
      val mockDateService = mock[DateService]

      when(mockDateService.getWeekendTrips(eqTo(6), eqTo(2024), eqTo(1))).thenReturn(List.empty)

      val controller = new ApiController(stubControllerComponents(), config) {
        override val tripCreator = mockTripCreator
        override val dateService = mockDateService
      }

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&month=6&year=2024&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[JsArray].value must be(empty)
    }

    "return BadRequest for invalid year format" in {
      val controller = new ApiController(stubControllerComponents(), config)

      val request = FakeRequest(GET, "/api/trips?fromCode=LHR&month=6&year=invalid&numberOfExtraDays=1")
      val result  = controller.getApiData()(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid number format for month, year, or numberOfExtraDays parameters")
    }

    "handle multiple departure airports" in {
      val mockTripCreator = mock[TripCreator]
      when(mockTripCreator.create(List("LHR", "LGW"), testDate, 2)).thenReturn(List(parisTrip))

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
      contentAsString(result1) must include(
        "Must provide either (date and numberOfDays) or (month, year, and numberOfExtraDays)"
      )

      val request2 = FakeRequest(GET, "/api/trips?fromCode=LHR&numberOfDays=2")
      val result2  = controller.getApiData()(request2)
      status(result2) mustBe BAD_REQUEST
      contentAsString(result2) must include(
        "Must provide either (date and numberOfDays) or (month, year, and numberOfExtraDays)"
      )

      val request3 = FakeRequest(GET, "/api/trips?date=2025-01-18&numberOfDays=2")
      val result3  = controller.getApiData()(request3)
      status(result3) mustBe BAD_REQUEST
      contentAsString(result3) must include("Missing required query parameter: fromCode")
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
