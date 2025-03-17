package services

import model.{Airport, Flight, Trip}
import org.mockito.ArgumentMatchers.{any, anyInt, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.time.{LocalDate, LocalDateTime, YearMonth}
import java.time.format.DateTimeFormatter

class AlternativeTripServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  private val mockDateService    = mock[DateService]
  private val mockTripCreator    = mock[TripCreator]
  private val mockWeekendService = mock[WeekendService]

  // Create a test subclass that we can use to test the service
  private class TestAlternativeTripService
      extends AlternativeTripService(mockDateService, mockTripCreator, mockWeekendService) {
    // Override getAlternativeWeekends to return a fixed list of trips for testing
    override def getAlternativeWeekends(
        origin: String,
        destination: String,
        yearMonth: YearMonth,
        extraDays: Int,
        selectedDepartureDay: LocalDate
    ): List[Trip] = {
      if (origin == "LHR" && destination == "BCN") {
        List(
          alternativeTrip1,
          alternativeTrip2,
          alternativeTrip3
        )
      } else {
        List.empty
      }
    }
  }

  private val service = new TestAlternativeTripService()

  private val origin          = "LHR"
  private val destination     = "BCN"
  private val monthStr        = "2025-06"
  private val year            = 2025
  private val month           = 6
  private val extraDays       = 0
  private val departureDayStr = "2025-06-07"

  private val departureDay = LocalDate.parse(departureDayStr, DateTimeFormatter.ISO_LOCAL_DATE)
  private val yearMonth    = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"))

  private val selectedTrip     = createTrip("LHR", "BCN", "2025-06-07", "2025-06-08", 100.0)
  private val alternativeTrip1 = createTrip("LHR", "BCN", "2025-06-14", "2025-06-15", 120.0)
  private val alternativeTrip2 = createTrip("LHR", "BCN", "2025-06-21", "2025-06-22", 110.0)
  private val alternativeTrip3 = createTrip("LHR", "BCN", "2025-06-28", "2025-06-29", 130.0)

  override def beforeEach(): Unit = {
    // Setup mock for TripCreator
    when(mockTripCreator.create(List(origin), departureDay, extraDays + 1))
      .thenReturn(List(selectedTrip))
  }

  "AlternativeTripService" should "return the selected trip and alternative weekends" in {
    val result = service.getAlternativeTrips(origin, destination, extraDays, departureDayStr)

    result.selected_trip shouldBe selectedTrip
    result.alternative_weekends should contain theSameElementsAs List(
      alternativeTrip1,
      alternativeTrip2,
      alternativeTrip3
    )
  }

  it should "throw an exception if no trips are found for the selected departure day" in {
    // Setup mock to return empty list for the selected departure day
    when(mockTripCreator.create(List(origin), departureDay, extraDays + 1))
      .thenReturn(List.empty)

    an[IllegalArgumentException] should be thrownBy {
      service.getAlternativeTrips(origin, destination, extraDays, departureDayStr)
    }
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
