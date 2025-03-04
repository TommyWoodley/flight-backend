package services

import javax.inject.{Inject, Singleton}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import model.{Flight, Airport}
import play.api.libs.json.Json

import scala.jdk.CollectionConverters._
import scala.util.{Try, Success, Failure}
import play.api.{Configuration, Logger}
import java.net.URI
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

@Singleton
class DynamoDBService @Inject() (config: Configuration) {
  private val logger = Logger(this.getClass)

  // LocalStack endpoint configuration
  private val localstackEndpoint = "http://localhost:4566"
  private val region             = Region.EU_WEST_1

  private val dynamoDbClient = DynamoDbClient
    .builder()
    .endpointOverride(URI.create(localstackEndpoint))
    .credentialsProvider(
      StaticCredentialsProvider.create(
        AwsBasicCredentials.create("dummy", "dummy")
      )
    )
    .region(region)
    .build()

  private val flightsTableName = "Flights"

  def createFlightsTable(): Try[CreateTableResponse] = Try {
    val request = CreateTableRequest
      .builder()
      .tableName(flightsTableName)
      .attributeDefinitions(
        AttributeDefinition
          .builder()
          .attributeName("routeDate")
          .attributeType(ScalarAttributeType.S)
          .build(),
        AttributeDefinition
          .builder()
          .attributeName("flightNumber")
          .attributeType(ScalarAttributeType.S)
          .build()
      )
      .keySchema(
        KeySchemaElement
          .builder()
          .attributeName("routeDate")
          .keyType(KeyType.HASH)
          .build(),
        KeySchemaElement
          .builder()
          .attributeName("flightNumber")
          .keyType(KeyType.RANGE)
          .build()
      )
      .provisionedThroughput(
        ProvisionedThroughput
          .builder()
          .readCapacityUnits(5L)
          .writeCapacityUnits(5L)
          .build()
      )
      .build()

    dynamoDbClient.createTable(request)
  }

  def storeFlights(flights: List[Flight], date: LocalDate): Try[Unit] = Try {
    flights.foreach { flight =>
      val routeDate = s"${flight.departureCode}#${flight.arrivalCode}#${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

      val item = Map(
        "routeDate"     -> AttributeValue.builder().s(routeDate).build(),
        "flightNumber"  -> AttributeValue.builder().s(flight.flightNumber).build(),
        "airline"       -> AttributeValue.builder().s(flight.airline).build(),
        "departureCode" -> AttributeValue.builder().s(flight.departureCode).build(),
        "arrivalCode"   -> AttributeValue.builder().s(flight.arrivalCode).build(),
        "departureTime" -> AttributeValue
          .builder()
          .s(flight.departureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .build(),
        "arrivalTime"   -> AttributeValue
          .builder()
          .s(flight.arrivalTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .build(),
        "price"         -> AttributeValue.builder().n(flight.price.toString).build()
      ).asJava

      val request = PutItemRequest
        .builder()
        .tableName(flightsTableName)
        .item(item)
        .build()

      dynamoDbClient.putItem(request)
    }
  }

  def getFlights(from: Airport, to: Airport, date: LocalDate): Try[List[Flight]] = Try {
    val routeDate = s"${from.skyId}#${to.skyId}#${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

    val request = QueryRequest
      .builder()
      .tableName(flightsTableName)
      .keyConditionExpression("routeDate = :rd")
      .expressionAttributeValues(
        Map(":rd" -> AttributeValue.builder().s(routeDate).build()).asJava
      )
      .build()

    val response = dynamoDbClient.query(request)

    response.items().asScala.toList.map { item =>
      val map = item.asScala
      Flight(
        flightNumber = map("flightNumber").s(),
        airline = map("airline").s(),
        departureCode = map("departureCode").s(),
        arrivalCode = map("arrivalCode").s(),
        departureTime = LocalDateTime.parse(map("departureTime").s(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        arrivalTime = LocalDateTime.parse(map("arrivalTime").s(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        price = map("price").n().toDouble
      )
    }
  }

  def tableExists(): Try[Boolean] = Try {
    try {
      val request = DescribeTableRequest
        .builder()
        .tableName(flightsTableName)
        .build()
      dynamoDbClient.describeTable(request)
      true
    } catch {
      case _: ResourceNotFoundException => false
    }
  }
}
