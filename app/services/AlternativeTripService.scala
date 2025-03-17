package services

import model.{AlternativeTrips, Trip}
import play.api.Logger
import javax.inject.{Inject, Singleton}

import java.time.{LocalDate, YearMonth}
import java.time.format.DateTimeFormatter

@Singleton
class AlternativeTripService @Inject() (
    dateService: DateService,
    tripCreator: TripCreator,
    weekendService: WeekendService
) {
  private val logger: Logger = Logger(this.getClass)

  /** Get alternative weekend trips for a given origin, destination, month, and trip type
    *
    * @param origin
    *   IATA code of departure airport
    * @param destination
    *   IATA code of destination airport
    * @param extraDays
    *   0 for weekend, 1 for long-weekend
    * @param departureDayStr
    *   The user-selected weekend departure date in YYYY-MM-DD format
    * @return
    *   AlternativeTrips object containing the selected trip and alternative weekends
    */
  def getAlternativeTrips(
      origin: String,
      destination: String,
      extraDays: Int,
      departureDayStr: String
  ): AlternativeTrips = {
    // Parse the departure day
    val departureDay = LocalDate.parse(departureDayStr, DateTimeFormatter.ISO_LOCAL_DATE)

    // Extract month and year from the departure day
    val month = departureDay.getMonthValue
    val year  = departureDay.getYear

    logger.info(s"Getting alternative trips for $origin to $destination in $month/$year with $extraDays extra days")

    // Create the YearMonth from the departure day
    val yearMonth = YearMonth.of(year, month)

    // Get the selected trip
    val selectedTrip = getSelectedTrip(origin, destination, departureDay, extraDays)

    // Get alternative weekends in the same month
    val alternativeWeekends = getAlternativeWeekends(origin, destination, yearMonth, extraDays, departureDay)

    AlternativeTrips(
      selected_trip = selectedTrip,
      alternative_weekends = alternativeWeekends
    )
  }

  /** Get the selected trip based on the departure day and extra days
    */
  private def getSelectedTrip(
      origin: String,
      destination: String,
      departureDay: LocalDate,
      extraDays: Int
  ): Trip = {
    val trips = tripCreator
      .create(
        List(origin),
        departureDay,
        extraDays + 1 // +1 because extraDays is additional to the weekend
      )
      .filter(_.outbound.arrivalCode == destination)

    if (trips.isEmpty) {
      throw new IllegalArgumentException(s"No trips found from $origin to $destination on $departureDay")
    }

    trips.minBy(_.totalPrice)
  }

  /** Get alternative weekends in the same month
    */
  protected def getAlternativeWeekends(
      origin: String,
      destination: String,
      yearMonth: YearMonth,
      extraDays: Int,
      selectedDepartureDay: LocalDate
  ): List[Trip] = {
    // Get all weekend trip dates in the month
    val tripDates = dateService.getWeekendTrips(yearMonth.getMonthValue, yearMonth.getYear, extraDays)

    // Filter out the selected departure day
    val alternativeTripDates = tripDates.filter(_.startDate != selectedDepartureDay)

    // Find trips for each possible date combination
    val allTrips = alternativeTripDates.flatMap { tripDate =>
      tripCreator
        .create(
          List(origin),
          tripDate.startDate,
          extraDays + 1 // +1 because extraDays is additional to the weekend
        )
        .filter(_.outbound.arrivalCode == destination)
    }

    // Get the cheapest trip for each weekend
    allTrips
      .groupBy(trip => trip.outbound.departureTime.toLocalDate)
      .values
      .flatMap(_.minByOption(_.totalPrice))
      .toList
      .sortBy(_.outbound.departureTime)
  }
}
