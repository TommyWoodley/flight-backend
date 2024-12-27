package cache

import play.api.libs.json.JsValue
import services.ApiService

class CachingApiService(delegate: ApiService, cache: Cache) extends ApiService {

  override def get(endpoint: String, params: Map[String, String]): Option[JsValue] = {
    val cacheKey = s"$endpoint?${params.mkString("&")}"

    cache.get(cacheKey) match {
      case Some(cachedResponse) => Some(cachedResponse)
      case None =>
        val response = delegate.get(endpoint, params)
        response.foreach(cache.put(cacheKey, _))
        response
    }
  }
}