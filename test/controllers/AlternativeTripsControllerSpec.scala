package controllers

import model.{AlternativeTrips, Flight, Trip}
import org.mockito.ArgumentMatchers.{any, anyInt, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Results}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import services._

import java.time.LocalDateTime

class AlternativeTripsControllerSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with Results {

  private val mockAirportService         = mock[AirportService]
  private val mockDateService            = mock[DateService]
  private val mockFlightService          = mock[FlightService]
  private val mockWeekendService         = mock[WeekendService]
  private val mockTripCreator            = mock[TripCreator]
  private val mockAlternativeTripService = mock[AlternativeTripService]
  private val mockConfig                 = mock[Configuration]

  private val cc = Helpers.stubControllerComponents()

  private val controller = new ApiController(
    cc,
    mockAirportService,
    mockDateService,
    mockFlightService,
    mockWeekendService,
    mockTripCreator,
    mockAlternativeTripService,
    mockConfig
  )

  private val origin       = "LHR"
  private val destination  = "CDG"
  private val month        = "2025-06"
  private val year         = 2025
  private val monthValue   = 6
  private val extraDays    = 0
  private val departureDay = "2025-06-14"

  private val selectedTrip     = createTrip("LHR", "BCN", "2025-06-07", "2025-06-08", 100.0)
  private val alternativeTrip1 = createTrip("LHR", "BCN", "2025-06-14", "2025-06-15", 120.0)
  private val alternativeTrip2 = createTrip("LHR", "BCN", "2025-06-21", "2025-06-22", 110.0)

  private val alternativeTrips = AlternativeTrips(
    selected_trip = selectedTrip,
    alternative_weekends = List(alternativeTrip1, alternativeTrip2)
  )

  override def beforeEach(): Unit = {
    when(
      mockAlternativeTripService.getAlternativeTrips(
        eqTo(origin),
        eqTo(destination),
        eqTo(extraDays),
        eqTo(departureDay)
      )
    ).thenReturn(alternativeTrips)
  }

  "ApiController.getAlternativeTrips" should "return 200 OK with alternative trips" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=$extraDays&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe OK
    contentAsJson(result) shouldBe Json.toJson(alternativeTrips)
  }

  it should "return 400 Bad Request when origin is missing" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?destination=$destination&extra_days=$extraDays&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Missing required query parameter: origin")
  }

  it should "return 400 Bad Request when destination is missing" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&extra_days=$extraDays&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Missing required query parameter: destination")
  }

  it should "return 400 Bad Request when extra_days is missing" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Missing required query parameter: extra_days")
  }

  it should "return 400 Bad Request when departure_day is missing" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=$extraDays"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Missing required query parameter: departure_day")
  }

  it should "return 400 Bad Request when extra_days is invalid" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=invalid&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Invalid number format for extra_days parameter")
  }

  it should "return 400 Bad Request when extra_days is out of range" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=2&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("extra_days must be 0 (for weekend) or 1 (for long-weekend)")
  }

  it should "return 400 Bad Request when departure_day format is invalid" in {
    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=$extraDays&departure_day=invalid"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Invalid departure_day format")
  }

  it should "return 400 Bad Request when service throws IllegalArgumentException" in {
    when(
      mockAlternativeTripService.getAlternativeTrips(
        eqTo(origin),
        eqTo(destination),
        eqTo(extraDays),
        eqTo(departureDay)
      )
    ).thenThrow(new IllegalArgumentException("Test error"))

    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=$extraDays&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should include("Test error")
  }

  it should "return 500 Internal Server Error when service throws Exception" in {
    when(
      mockAlternativeTripService.getAlternativeTrips(
        eqTo(origin),
        eqTo(destination),
        eqTo(extraDays),
        eqTo(departureDay)
      )
    ).thenThrow(new RuntimeException("Test error"))

    val request = FakeRequest(
      GET,
      s"/api/trips/alternatives?origin=$origin&destination=$destination&extra_days=$extraDays&departure_day=$departureDay"
    )
    val result  = controller.getAlternativeTrips()(request)

    status(result) shouldBe INTERNAL_SERVER_ERROR
    contentAsString(result) should include("Error getting alternative trips")
  }

  private def createTrip(from: String, to: String, departureDate: String, returnDate: String, price: Double): Trip = {
    val outboundFlight = Flight(
      flightNumber = "BA123",
      airline = "British Airways",
      departureCode = from,
      arrivalCode = to,
      departureTime = LocalDateTime.parse(s"${departureDate}T10:00:00"),
      arrivalTime = LocalDateTime.parse(s"${departureDate}T12:00:00"),
      price = price / 2
    )

    val inboundFlight = Flight(
      flightNumber = "BA456",
      airline = "British Airways",
      departureCode = to,
      arrivalCode = from,
      departureTime = LocalDateTime.parse(s"${returnDate}T18:00:00"),
      arrivalTime = LocalDateTime.parse(s"${returnDate}T20:00:00"),
      price = price / 2
    )

    Trip(to, outboundFlight, inboundFlight)
  }
}
