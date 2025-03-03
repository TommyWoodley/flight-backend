package services

import java.time.{DayOfWeek, LocalDate, YearMonth}
import scala.collection.mutable.ListBuffer

class DateService {
  case class TripDates(startDate: LocalDate, endDate: LocalDate)

  def getWeekendTrips(month: Int, year: Int, additionalDays: Int): List[TripDates] = {
    if (additionalDays > 2) {
      throw new IllegalArgumentException("Additional days cannot be more than 2")
    }

    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Month must be between 1 and 12")
    }

    val yearMonth = YearMonth.of(year, month)
    val result    = new ListBuffer[TripDates]()

    var currentDate = yearMonth.atDay(1)
    val endOfMonth  = yearMonth.atEndOfMonth()

    while (currentDate.isBefore(endOfMonth) || currentDate.isEqual(endOfMonth)) {
      val tripEndDate = currentDate.plusDays(1 + additionalDays)

      additionalDays match {
        case 0 => // Saturday-Sunday trips
          if (currentDate.getDayOfWeek == DayOfWeek.SATURDAY) {
            result += TripDates(currentDate, tripEndDate)
          }

        case 1 => // Friday-Sunday and Saturday-Monday trips
          if (currentDate.getDayOfWeek == DayOfWeek.FRIDAY) {
            result += TripDates(currentDate, tripEndDate) // Friday-Sunday
          } else if (currentDate.getDayOfWeek == DayOfWeek.SATURDAY) {
            result += TripDates(currentDate, tripEndDate) // Saturday-Monday
          }

        case 2 => // Thursday-Sunday, Friday-Monday, Saturday-Tuesday trips
          if (currentDate.getDayOfWeek == DayOfWeek.THURSDAY) {
            result += TripDates(currentDate, tripEndDate) // Thursday-Sunday
          } else if (currentDate.getDayOfWeek == DayOfWeek.FRIDAY) {
            result += TripDates(currentDate, tripEndDate) // Friday-Monday
          } else if (currentDate.getDayOfWeek == DayOfWeek.SATURDAY) {
            result += TripDates(currentDate, tripEndDate) // Saturday-Tuesday
          }
      }

      currentDate = currentDate.plusDays(1)
    }

    result.toList
  }

  private def findWeekendInTrip(startDate: LocalDate, additionalDays: Int): Set[LocalDate] = {
    val endDate = additionalDays match {
      case 0 => startDate.plusDays(1) // Sunday
      case 1 => startDate.plusDays(2) // Sunday or Monday
      case 2 => startDate.plusDays(3) // Sunday, Monday, or Tuesday
    }

    var currentDate  = startDate
    val weekendDates = new ListBuffer[LocalDate]()

    while (!currentDate.isAfter(endDate)) {
      if (
        currentDate.getDayOfWeek == DayOfWeek.SATURDAY ||
        currentDate.getDayOfWeek == DayOfWeek.SUNDAY
      ) {
        weekendDates += currentDate
      }
      currentDate = currentDate.plusDays(1)
    }

    weekendDates.toSet
  }
}
