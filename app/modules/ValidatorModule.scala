package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/** Module for validator-related bindings
  */
class ValidatorModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    // No bindings needed for companion objects
    // RequestValidator is a companion object that doesn't need explicit binding
  }
}
