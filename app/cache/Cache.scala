package cache

import play.api.libs.json.JsValue

import scala.collection.mutable

class Cache {
  private val cache: mutable.Map[String, JsValue] = mutable.Map()

  def get(key: String): Option[JsValue] = cache.get(key)

  def put(key: String, value: JsValue): Unit = cache.put(key, value)
}
