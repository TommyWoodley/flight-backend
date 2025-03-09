package services

import model.Trip
import play.api.Logger
import javax.inject.{Inject, Singleton}

@Singleton
class WeekendService @Inject() (
    dateService: DateService,
    tripCreator: TripCreator
) {
  private val logger: Logger = Logger(this.getClass)

  def getWeekendTrips(fromCodes: List[String], month: Int, year: Int, numberOfExtraDays: Int): List[Trip] = {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Month must be between 1 and 12")
    }
    if (numberOfExtraDays > 2) {
      throw new IllegalArgumentException("Additional days cannot be more than 2")
    }

    val tripDates = dateService.getWeekendTrips(month, year, numberOfExtraDays)

    // Find trips for each possible date combination and get the best ones
    val allTrips = tripDates.flatMap { tripDate =>
      tripCreator.create(
        fromCodes,
        tripDate.startDate,
        numberOfExtraDays + 1
      ) // +1 because numberOfExtraDays is additional to the weekend
    }

    // Group by destination and get the cheapest trip for each
    allTrips
      .groupBy(_.destination)
      .values
      .flatMap(_.minByOption(_.pricePerHour))
      .toList
      .sortBy(_.totalPrice)
  }
}
