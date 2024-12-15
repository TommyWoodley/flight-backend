package services

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import scalaj.http.Http

class HttpApiService extends ApiService {
  private val logger: Logger = Logger(this.getClass)
  private val flightLabsApiKey: String = sys.env.getOrElse("FLIGHT_LABS_API", "")
  private val baseUrl: String = "https://www.goflightlabs.com"

  override def get(endpoint: String, params: Map[String, String]): JsValue = {
    val fullUrl = s"$baseUrl$endpoint"
    val paramsWithKey = params + ("access_key" -> flightLabsApiKey)

    logger.info(s"Making request to $fullUrl with params $params")

    val response = Http(fullUrl)
      .params(paramsWithKey)
      .timeout(connTimeoutMs = 5000, readTimeoutMs = 20000)
      .asString
    Json.parse(response.body)
  }
}
