package validators

import play.api.mvc.Results

import java.time.LocalDate
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
      val fromCodes = parseCommaSeperatedList(fromCodeOpt.get)

      // Validate date and numberOfDays are present
      if (dateStrOpt.isEmpty || numberOfDaysStrOpt.isEmpty) {
        Failure(new IllegalArgumentException("Must provide both date and numberOfDays parameters"))
      } else {
        // Validate date format
        val dateTry = parseDate(dateStrOpt.get)

        // Validate numberOfDays format
        val numberOfDaysTry = parseStringToInt(numberOfDaysStrOpt.get, "numberOfDays")

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
      val fromCodes = parseCommaSeperatedList(fromCodeOpt.get)

      // Validate all required parameters are present
      if (monthStrOpt.isEmpty || yearStrOpt.isEmpty || numberOfExtraDaysStrOpt.isEmpty) {
        Failure(new IllegalArgumentException("Must provide month, year, and numberOfExtraDays parameters"))
      } else {
        // Validate month format
        val monthTry = parseMonth(monthStrOpt.get)

        // Validate year format
        val yearTry = parseYear(yearStrOpt.get)

        // Validate numberOfExtraDays format
        val numberOfExtraDaysTry = parseStringToInt(numberOfExtraDaysStrOpt.get, "numberOfExtraDays")

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
  private def parseCommaSeperatedList(codesStr: String): List[String] = {
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
    parseStringToInt(monthStr, "month") match {
      case Success(month) if month >= 1 && month <= 12 =>
        Success(month)
      case Success(_)                                  =>
        Failure(new IllegalArgumentException("Month must be between 1 and 12."))
      case Failure(e)                                  => Failure(e)
    }
  }

  /** Parses a year string to an integer
    *
    * @param yearStr
    *   String representation of a year
    * @return
    *   Try with either an Int or an exception
    */
  private def parseYear(yearStr: String): Try[Int] = parseStringToInt(yearStr, "year")

  /** Parses a string to an integer for a given parameter
    *
    * @param numberStr
    *   String representation of a number
    * @param paramName
    *   String paramteter name for the error message
    * @return
    *   Try with either an Int or an exception
    */
  private def parseStringToInt(numberStr: String, paramName: String): Try[Int] = {
    Try(numberStr.toInt).recoverWith { case _: NumberFormatException =>
      Failure(new IllegalArgumentException(s"Invalid number format for $paramName parameter"))
    }
  }
}
