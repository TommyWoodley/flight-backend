package cache

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import play.api.Configuration
import play.api.libs.json.JsValue
import services.ApiService

import java.util.concurrent.TimeUnit

class CachingApiService(delegate: ApiService)(implicit config: Configuration) extends ApiService {
  private val duration: Long = config.get[Long]("cache.expiry.duration")
  private val unit: TimeUnit = TimeUnit.valueOf(config.get[String]("cache.expiry.unit"))

  private val cache: LoadingCache[(String, Map[String, String]), Option[JsValue]] = CacheBuilder
    .newBuilder()
    .expireAfterWrite(duration, unit)
    .build(new CacheLoader[(String, Map[String, String]), Option[JsValue]]() {
      override def load(key: (String, Map[String, String])): Option[JsValue] = {
        val (endpoint, params) = key
        delegate.get(endpoint, params)
      }
    })

  override def get(endpoint: String, params: Map[String, String]): Option[JsValue] =
    cache.get((endpoint, params))
}
