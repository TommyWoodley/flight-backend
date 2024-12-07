package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  "HomeController GET" should {

    "return JSON data from the new API endpoint" in {
      val controller = new HomeController(stubControllerComponents())
      val apiData = controller.getApiData().apply(FakeRequest(GET, "/api/data"))

      status(apiData) mustBe OK
      contentType(apiData) mustBe Some("application/json")
      contentAsString(apiData) must include ("This is a sample API response")
    }
  }

}
