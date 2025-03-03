package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.LocalDate

class DateServiceSpec extends AnyFlatSpec with Matchers {
  val dateService = new DateService()
  import dateService.TripDates

  "DateService" should "return correct weekend trips for 0 additional days (Saturday-Sunday)" in {
    val trips = dateService.getWeekendTrips(6, 2024, 0) // June 2024

    // June 2024 has 5 weekends
    trips.size shouldBe 5

    // Verify all expected Saturday-Sunday combinations
    trips should contain theSameElementsAs List(
      TripDates(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 2)),
      TripDates(LocalDate.of(2024, 6, 8), LocalDate.of(2024, 6, 9)),
      TripDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 16)),
      TripDates(LocalDate.of(2024, 6, 22), LocalDate.of(2024, 6, 23)),
      TripDates(LocalDate.of(2024, 6, 29), LocalDate.of(2024, 6, 30))
    )
  }

  it should "return correct trips for 1 additional day (Friday-Sunday or Saturday-Monday)" in {
    val trips = dateService.getWeekendTrips(6, 2024, 1)

    // June 2024 will have both Friday-Sunday and Saturday-Monday trips
    trips.size shouldBe 9

    // Verify all expected combinations
    trips should contain theSameElementsAs List(
      // First weekend
      TripDates(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 3)),   // Sat-Mon
      // Second weekend
      TripDates(LocalDate.of(2024, 6, 7), LocalDate.of(2024, 6, 9)),   // Fri-Sun
      TripDates(LocalDate.of(2024, 6, 8), LocalDate.of(2024, 6, 10)),  // Sat-Mon
      // Third weekend
      TripDates(LocalDate.of(2024, 6, 14), LocalDate.of(2024, 6, 16)), // Fri-Sun
      TripDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 17)), // Sat-Mon
      // Fourth weekend
      TripDates(LocalDate.of(2024, 6, 21), LocalDate.of(2024, 6, 23)), // Fri-Sun
      TripDates(LocalDate.of(2024, 6, 22), LocalDate.of(2024, 6, 24)), // Sat-Mon
      // Fifth weekend
      TripDates(LocalDate.of(2024, 6, 28), LocalDate.of(2024, 6, 30)), // Fri-Sun
      TripDates(LocalDate.of(2024, 6, 29), LocalDate.of(2024, 7, 1))   // Sat-Mon
    )
  }

  it should "return correct trips for 2 additional days" in {
    val trips = dateService.getWeekendTrips(6, 2024, 2)

    // June 2024 will have Thursday-Sunday, Friday-Monday, and Saturday-Tuesday trips
    trips.size shouldBe 13

    // Verify all expected combinations
    trips should contain theSameElementsAs List(
      // First weekend
      TripDates(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 4)),   // Sat-Tue
      // Second weekend
      TripDates(LocalDate.of(2024, 6, 6), LocalDate.of(2024, 6, 9)),   // Thu-Sun
      TripDates(LocalDate.of(2024, 6, 7), LocalDate.of(2024, 6, 10)),  // Fri-Mon
      TripDates(LocalDate.of(2024, 6, 8), LocalDate.of(2024, 6, 11)),  // Sat-Tue
      // Third weekend
      TripDates(LocalDate.of(2024, 6, 13), LocalDate.of(2024, 6, 16)), // Thu-Sun
      TripDates(LocalDate.of(2024, 6, 14), LocalDate.of(2024, 6, 17)), // Fri-Mon
      TripDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 18)), // Sat-Tue
      // Fourth weekend
      TripDates(LocalDate.of(2024, 6, 20), LocalDate.of(2024, 6, 23)), // Thu-Sun
      TripDates(LocalDate.of(2024, 6, 21), LocalDate.of(2024, 6, 24)), // Fri-Mon
      TripDates(LocalDate.of(2024, 6, 22), LocalDate.of(2024, 6, 25)), // Sat-Tue
      // Fifth weekend
      TripDates(LocalDate.of(2024, 6, 27), LocalDate.of(2024, 6, 30)), // Thu-Sun
      TripDates(LocalDate.of(2024, 6, 28), LocalDate.of(2024, 7, 1)),  // Fri-Mon
      TripDates(LocalDate.of(2024, 6, 29), LocalDate.of(2024, 7, 2))   // Sat-Tue
    )
  }

  it should "throw IllegalArgumentException for more than 2 additional days" in {
    val exception = intercept[IllegalArgumentException] {
      dateService.getWeekendTrips(6, 2024, 3)
    }
    exception.getMessage should include("Additional days cannot be more than 2")
  }

  it should "throw IllegalArgumentException for invalid month" in {
    val exception = intercept[IllegalArgumentException] {
      dateService.getWeekendTrips(13, 2024, 0)
    }
    exception.getMessage should include("Month must be between 1 and 12")
  }

  it should "handle months with partial weeks at the end" in {
    // July 2024 ends on Wednesday
    val trips = dateService.getWeekendTrips(7, 2024, 2)

    // Verify all trips start in July
    trips.foreach { trip =>
      trip.startDate.getMonthValue shouldBe 7
    }
  }

  it should "handle February in leap year" in {
    val trips = dateService.getWeekendTrips(2, 2024, 0)

    // Verify all trips start in February
    trips.foreach { trip =>
      trip.startDate.getMonthValue shouldBe 2
    }
  }

  it should "handle February in non-leap year" in {
    val trips = dateService.getWeekendTrips(2, 2023, 0)

    // Verify all trips start in February
    trips.foreach { trip =>
      trip.startDate.getMonthValue shouldBe 2
    }
  }
}
