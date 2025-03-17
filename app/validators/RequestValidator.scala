package validators

import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

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
}
