package validators

import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

import java.time.{LocalDate, YearMonth}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.util.{Failure, Success, Try}

/** Companion object for validating API requests. Contains utility methods for validating request parameters.
  */
object RequestValidator extends Results {

  /** Validates trip request parameters
    *
    * @param fromCodeOpt
    *   Option containing the fromCode parameter
    * @param dateStrOpt
    *   Option containing the date parameter
    * @param numberOfDaysStrOpt
    *   Option containing the numberOfDays parameter
    * @return
    *   Try with either a validation Result (for failure) or a tuple with validated parameters (for success)
    */
  def validateTripRequest(
      fromCodeOpt: Option[String],
      dateStrOpt: Option[String],
      numberOfDaysStrOpt: Option[String]
  ): Try[(List[String], LocalDate, Int)] = {
    // Validate fromCode is present
    if (fromCodeOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: fromCode"))
    } else {
      val fromCodes = parseCodes(fromCodeOpt.get)

      // Validate date and numberOfDays are present
      if (dateStrOpt.isEmpty || numberOfDaysStrOpt.isEmpty) {
        Failure(new IllegalArgumentException("Must provide both date and numberOfDays parameters"))
      } else {
        // Validate date format
        val dateTry = parseDate(dateStrOpt.get)

        // Validate numberOfDays format
        val numberOfDaysTry = parseNumberOfDays(numberOfDaysStrOpt.get)

        // Combine the results
        for {
          date         <- dateTry
          numberOfDays <- numberOfDaysTry
        } yield (fromCodes, date, numberOfDays)
      }
    }
  }

  /** Validates weekend request parameters
    *
    * @param fromCodeOpt
    *   Option containing the fromCode parameter
    * @param monthStrOpt
    *   Option containing the month parameter
    * @param yearStrOpt
    *   Option containing the year parameter
    * @param numberOfExtraDaysStrOpt
    *   Option containing the numberOfExtraDays parameter
    * @return
    *   Try with either a validation Result (for failure) or a tuple with validated parameters (for success)
    */
  def validateWeekendRequest(
      fromCodeOpt: Option[String],
      monthStrOpt: Option[String],
      yearStrOpt: Option[String],
      numberOfExtraDaysStrOpt: Option[String]
  ): Try[(List[String], Int, Int, Int)] = {
    // Validate fromCode is present
    if (fromCodeOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: fromCode"))
    } else {
      val fromCodes = parseCodes(fromCodeOpt.get)

      // Validate all required parameters are present
      if (monthStrOpt.isEmpty || yearStrOpt.isEmpty || numberOfExtraDaysStrOpt.isEmpty) {
        Failure(new IllegalArgumentException("Must provide month, year, and numberOfExtraDays parameters"))
      } else {
        // Validate month format
        val monthTry = parseMonth(monthStrOpt.get)

        // Validate year format
        val yearTry = parseYear(yearStrOpt.get)

        // Validate numberOfExtraDays format
        val numberOfExtraDaysTry = parseNumberOfExtraDays(numberOfExtraDaysStrOpt.get)

        // Combine the results
        for {
          month             <- monthTry
          year              <- yearTry
          numberOfExtraDays <- numberOfExtraDaysTry
        } yield (fromCodes, month, year, numberOfExtraDays)
      }
    }
  }

  /** Parses a comma-separated list of codes into a List
    *
    * @param codesStr
    *   String containing comma-separated codes
    * @return
    *   List of codes
    */
  private def parseCodes(codesStr: String): List[String] = {
    if (codesStr.isEmpty) List.empty else codesStr.split(",").toList
  }

  /** Parses a date string to LocalDate
    *
    * @param dateStr
    *   String date in ISO_LOCAL_DATE format
    * @return
    *   Try with either a LocalDate or an exception
    */
  private def parseDate(dateStr: String): Try[LocalDate] = {
    Try(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)).recoverWith { case _: DateTimeParseException =>
      Failure(new IllegalArgumentException("Invalid date format. Please use ISO_LOCAL_DATE format (yyyy-MM-dd)."))
    }
  }

  /** Parses a month string to an integer
    *
    * @param monthStr
    *   String representation of a month (1-12)
    * @return
    *   Try with either an Int or an exception
    */
  private def parseMonth(monthStr: String): Try[Int] = {
    Try(monthStr.toInt)
      .flatMap { month =>
        if (month >= 1 && month <= 12) {
          Success(month)
        } else {
          Failure(new IllegalArgumentException("Month must be between 1 and 12."))
        }
      }
      .recoverWith { case _: NumberFormatException =>
        Failure(new IllegalArgumentException("Invalid number format for month, year, or numberOfExtraDays parameters"))
      }
  }

  /** Parses a year string to an integer
    *
    * @param yearStr
    *   String representation of a year
    * @return
    *   Try with either an Int or an exception
    */
  private def parseYear(yearStr: String): Try[Int] = {
    Try(yearStr.toInt).recoverWith { case _: NumberFormatException =>
      Failure(new IllegalArgumentException("Invalid number format for month, year, or numberOfExtraDays parameters"))
    }
  }

  /** Parses a string to an integer
    *
    * @param numberStr
    *   String representation of a number
    * @return
    *   Try with either an Int or an exception
    */
  private def parseNumberOfDays(numberStr: String): Try[Int] = {
    Try(numberStr.toInt).recoverWith { case _: NumberFormatException =>
      Failure(new IllegalArgumentException("Invalid number format for numberOfDays parameter."))
    }
  }

  /** Parses a string to an integer for the numberOfExtraDays parameter in weekends request
    *
    * @param numberStr
    *   String representation of a number
    * @return
    *   Try with either an Int or an exception
    */
  private def parseNumberOfExtraDays(numberStr: String): Try[Int] = {
    Try(numberStr.toInt).recoverWith { case _: NumberFormatException =>
      Failure(new IllegalArgumentException("Invalid number format for month, year, or numberOfExtraDays parameters"))
    }
  }

  /** Validates alternative trips request parameters
    *
    * @param originOpt
    *   Option containing the origin parameter
    * @param destinationOpt
    *   Option containing the destination parameter
    * @param monthStrOpt
    *   Option containing the month parameter
    * @param extraDaysStrOpt
    *   Option containing the extra_days parameter
    * @param departureDayStrOpt
    *   Option containing the departure_day parameter
    * @return
    *   Try with either a validation Result (for failure) or a tuple with validated parameters (for success)
    */
  def validateAlternativeTripsRequest(
      originOpt: Option[String],
      destinationOpt: Option[String],
      monthStrOpt: Option[String],
      extraDaysStrOpt: Option[String],
      departureDayStrOpt: Option[String]
  ): Try[(String, String, String, Int, String)] = {
    // Check if all required parameters are present
    if (originOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: origin"))
    } else if (destinationOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: destination"))
    } else if (monthStrOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: month"))
    } else if (extraDaysStrOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: extra_days"))
    } else if (departureDayStrOpt.isEmpty) {
      Failure(new IllegalArgumentException("Missing required query parameter: departure_day"))
    } else {
      // Validate extra_days format and range
      val extraDaysTry = parseExtraDays(extraDaysStrOpt.get)

      // Validate month format (YYYY-MM)
      val monthTry = parseYearMonth(monthStrOpt.get)

      // Validate departure_day format (YYYY-MM-DD)
      val departureDayTry = parseDepartureDay(departureDayStrOpt.get)

      // Combine the results
      for {
        extraDays    <- extraDaysTry
        month        <- monthTry
        departureDay <- departureDayTry
      } yield (originOpt.get, destinationOpt.get, month, extraDays, departureDay)
    }
  }

  /** Parses and validates extra_days parameter
    *
    * @param extraDaysStr
    *   String representation of extra_days
    * @return
    *   Try with either an Int or an exception
    */
  private def parseExtraDays(extraDaysStr: String): Try[Int] = {
    Try(extraDaysStr.toInt)
      .flatMap { extraDays =>
        if (extraDays >= 0 && extraDays <= 1) {
          Success(extraDays)
        } else {
          Failure(new IllegalArgumentException("extra_days must be 0 (for weekend) or 1 (for long-weekend)"))
        }
      }
      .recoverWith { case _: NumberFormatException =>
        Failure(new IllegalArgumentException("Invalid number format for extra_days parameter."))
      }
  }

  /** Validates the format of a month string (YYYY-MM)
    *
    * @param monthStr
    *   String month in YYYY-MM format
    * @return
    *   Try with either the validated month string or an exception
    */
  private def parseYearMonth(monthStr: String): Try[String] = {
    if (monthStr.matches("\\d{4}-\\d{2}")) {
      Try {
        // Further validate by parsing it to ensure it's a valid year-month
        YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"))
        monthStr
      }.recoverWith { case _: DateTimeParseException =>
        Failure(new IllegalArgumentException("Invalid month format. Please use YYYY-MM format."))
      }
    } else {
      Failure(new IllegalArgumentException("Invalid month format. Please use YYYY-MM format."))
    }
  }

  /** Validates the format of a departure day string (YYYY-MM-DD)
    *
    * @param departureDayStr
    *   String date in YYYY-MM-DD format
    * @return
    *   Try with either the validated departure day string or an exception
    */
  private def parseDepartureDay(departureDayStr: String): Try[String] = {
    if (departureDayStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
      Try {
        // Further validate by parsing it to ensure it's a valid date
        LocalDate.parse(departureDayStr, DateTimeFormatter.ISO_LOCAL_DATE)
        departureDayStr
      }.recoverWith { case _: DateTimeParseException =>
        Failure(new IllegalArgumentException("Invalid departure_day format. Please use YYYY-MM-DD format."))
      }
    } else {
      Failure(new IllegalArgumentException("Invalid departure_day format. Please use YYYY-MM-DD format."))
    }
  }
}
