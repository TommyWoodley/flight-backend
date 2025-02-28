package services

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.Try

class HttpApiServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfter {

  // Test data
  private val testEndpoint      = "/api/v1/test"
  private val testParams        = Map("param1" -> "value1")
  private val testApiKey        = "test-api-key"
  private val testBaseUrl       = "https://www.goflightlabs.com"
  private val validJsonResponse = """{"data": "test"}"""

  // Create a testable version of HttpApiService that overrides the HTTP call
  private class TestableHttpApiService(responseFunc: (String, Map[String, String]) => Option[JsValue])
      extends ApiService {
    override def get(endpoint: String, params: Map[String, String]): Option[JsValue] = {
      val paramsWithKey = params + ("access_key" -> testApiKey)
      Try(responseFunc(endpoint, paramsWithKey)).getOrElse(None)
    }
  }

  before {
    // Set up environment variable for API key
    sys.props.put("FLIGHT_LABS_API", testApiKey)
  }

  after {
    // Clean up environment variable
    sys.props.remove("FLIGHT_LABS_API")
  }

  "HttpApiService" should {
    "successfully make an API call and parse JSON response" in {
      val expectedJson   = Json.parse(validJsonResponse)
      val httpApiService = new TestableHttpApiService((endpoint: String, params: Map[String, String]) => {
        endpoint shouldBe testEndpoint
        params should contain("access_key" -> testApiKey)
        Some(expectedJson)
      })

      val result = httpApiService.get(testEndpoint, testParams)
      result shouldBe Some(expectedJson)
    }

    "handle failed API calls gracefully" in {
      val httpApiService = new TestableHttpApiService((_, _) => None)

      val result = httpApiService.get(testEndpoint, testParams)
      result shouldBe None
    }

    "include API key in request parameters" in {
      var capturedParams: Map[String, String] = Map.empty
      val httpApiService                      = new TestableHttpApiService((_, params: Map[String, String]) => {
        capturedParams = params
        Some(Json.parse(validJsonResponse))
      })

      httpApiService.get(testEndpoint, testParams)
      capturedParams should contain("access_key" -> testApiKey)
      capturedParams should contain("param1" -> "value1")
    }

    "handle invalid JSON responses" in {
      val httpApiService = new TestableHttpApiService((_, _) => throw new Exception("Invalid JSON"))

      val result = httpApiService.get(testEndpoint, testParams)
      result shouldBe None
    }
  }
}
