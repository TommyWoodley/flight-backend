package cache

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import play.api.libs.json.JsValue
import services.ApiService

import java.util.concurrent.TimeUnit

class CachingApiService(delegate: ApiService) extends ApiService {
  private val cache: LoadingCache[(String, Map[String, String]), Option[JsValue]] = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build(new CacheLoader[(String, Map[String, String]), Option[JsValue]]() {
      override def load(key: (String, Map[String, String])): Option[JsValue] = {
        val (endpoint, params) = key
        delegate.get(endpoint, params)
      }
    })

  override def get(endpoint: String, params: Map[String, String]): Option[JsValue] =
    cache.get((endpoint, params))
}