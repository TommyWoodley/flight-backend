package modules

import com.google.inject.AbstractModule
import services.DynamoDBService
import play.api.{Configuration, Environment}
import javax.inject.Singleton

class DynamoDBModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DynamoDBService]).in(classOf[Singleton])
  }
}
