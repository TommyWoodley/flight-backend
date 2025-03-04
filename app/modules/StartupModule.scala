package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment, Logger}
import services.DynamoDBService
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.inject.ApplicationLifecycle
import scala.util.{Success, Failure}

class StartupModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[StartupService]).asEagerSingleton()
  }
}

@Singleton
class StartupService @Inject() (
    dynamoDBService: DynamoDBService,
    lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // Initialize DynamoDB table
  dynamoDBService.tableExists() match {
    case Success(exists) if !exists =>
      logger.info("Creating DynamoDB Flights table...")
      dynamoDBService.createFlightsTable() match {
        case Success(_) => logger.info("DynamoDB Flights table created successfully")
        case Failure(e) => logger.error("Failed to create DynamoDB Flights table", e)
      }
    case Success(_)                 =>
      logger.info("DynamoDB Flights table already exists")
    case Failure(e)                 =>
      logger.error("Failed to check if DynamoDB Flights table exists", e)
  }

  // Clean up resources if needed
  lifecycle.addStopHook { () =>
    logger.info("Shutting down StartupService")
    scala.concurrent.Future.successful(())
  }
}
