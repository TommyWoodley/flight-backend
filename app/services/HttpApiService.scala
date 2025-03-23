package services

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import scalaj.http.{Http, HttpResponse}

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import utils.SecretsManagerUtil

import scala.util.{Failure, Success, Try}

@Singleton
class HttpApiService @Inject() (config: Configuration) extends ApiService {
  private val logger: Logger           = Logger(this.getClass)
  private val flightLabsApiKey: Try[String] = SecretsManagerUtil.getSecret("flight_labs_api")
  private val baseUrl: String          = "https://www.goflightlabs.com"

  override def get(endpoint: String, params: Map[String, String]): Option[JsValue] = {
    val key = flightLabsApiKey match {
      case Success(key) => key
      case Failure(exception) =>
        logger.error(s"Failed to get flight labs api key", exception)
        throw exception
    }
    val fullUrl       = s"$baseUrl$endpoint"
    val paramsWithKey = params + ("access_key" -> key)
    val threadName    = Thread.currentThread().getName

    logger.info(s"$threadName: Making request to $fullUrl with params $params")

    Try {
      val response: HttpResponse[String] = Http(fullUrl)
        .params(paramsWithKey)
        .timeout(connTimeoutMs = 5000, readTimeoutMs = 20000)
        .asString
      Json.parse(response.body)
    } match {
      case Success(json)      => Some(json)
      case Failure(exception) =>
        logger.error(s"$threadName: Failed to make request to $fullUrl with params $params", exception)
        None
    }
  }
}
