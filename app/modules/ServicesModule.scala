package modules

import com.google.inject.AbstractModule
import services._
import javax.inject.Singleton

class ServicesModule extends AbstractModule {
  override def configure(): Unit = {
    // Bind ApiService implementation directly to HttpApiService
    bind(classOf[ApiService])
      .to(classOf[HttpApiService])
      .in(classOf[Singleton])

    // Bind other services
    bind(classOf[AirportService]).in(classOf[Singleton])
    bind(classOf[DateService]).in(classOf[Singleton])
    bind(classOf[FlightService]).in(classOf[Singleton])
    bind(classOf[WeekendService]).in(classOf[Singleton])
    bind(classOf[TripCreator]).in(classOf[Singleton])
  }
}
