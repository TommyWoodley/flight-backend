package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import services.{AirportService, FlightService, TripCreator}
import model.{Airport, Flight, Trip}

import java.time.{LocalDate, LocalDateTime}

class TripCreatorSpec extends AnyFlatSpec with Matchers with MockitoSugar {
  val londonGatwickFromCode  = "LGW"
  val londonHeathrowFromCode = "LHR"
  val londonGatwickAirport   =
    Airport("LGW", "London Gatwick Airport", "LGW", "95565051", "United Kingdom", 51.1537, -0.1821)
  val londonHeathrowAirport  = Airport("LHR", "London Heathrow", "LHR", "95565041", "United Kingdom", 51.4700, -0.4543)
  val parisOrlyAirport       = Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565040", "France", 49.0097, 2.5479)

  val flightService  = mock[FlightService]
  val airportService = mock[AirportService]
  val tripCreator    = new TripCreator(flightService, airportService)

  val date         = LocalDate.of(2025, 1, 18)
  val numberOfDays = 2

  "TripCreator" should "create trips from a single set of flights" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode))).thenReturn(List(londonGatwickAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    val outboundFlight1 = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight1  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 18, 0),
      LocalDateTime.of(2025, 1, 20, 20, 0),
      100
    )

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight1))
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight1))

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips should have size 1
    trips should contain(Trip("France", outboundFlight1, inboundFlight1))
  }

  it should "handle no available flights" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode))).thenReturn(List(londonGatwickAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(Nil)
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays))).thenReturn(Nil)

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips shouldBe empty
  }

  it should "create trips from multiple destination airports" in {
    val parisCharlesDeGaulleAirport =
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479)
    val berlinTegelAirport          = Airport("TXL", "Berlin Tegel", "TXL", "95565042", "Germany", 52.5588, 13.2884)

    when(airportService.getAirportsByCode(List(londonGatwickFromCode))).thenReturn(List(londonGatwickAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisCharlesDeGaulleAirport, berlinTegelAirport))

    val outboundFlight1 = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight1  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 18, 0),
      LocalDateTime.of(2025, 1, 20, 20, 0),
      100
    )
    val outboundFlight2 = Flight(
      "flight456",
      "SpeedyJet",
      "LGW",
      "TXL",
      LocalDateTime.of(2025, 1, 18, 9, 0),
      LocalDateTime.of(2025, 1, 18, 11, 0),
      150
    )
    val inboundFlight2  = Flight(
      "flight678",
      "AB Airlines",
      "TXL",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 19, 0),
      LocalDateTime.of(2025, 1, 20, 21, 0),
      150
    )

    when(flightService.getFlights(londonGatwickAirport, parisCharlesDeGaulleAirport, date))
      .thenReturn(List(outboundFlight1))
    when(flightService.getFlights(parisCharlesDeGaulleAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight1))
    when(flightService.getFlights(londonGatwickAirport, berlinTegelAirport, date)).thenReturn(List(outboundFlight2))
    when(flightService.getFlights(berlinTegelAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight2))

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips should have size 2
    trips should contain(Trip("France", outboundFlight1, inboundFlight1))
    trips should contain(Trip("Germany", outboundFlight2, inboundFlight2))
  }

  it should "handle flights with overlapping times" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode))).thenReturn(List(londonGatwickAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    val outboundFlight = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 18, 9, 0),
      LocalDateTime.of(2025, 1, 18, 11, 0),
      100
    )

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight))
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight))

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips shouldBe empty
  }

  it should "handle no destination airports" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode))).thenReturn(List(londonGatwickAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country)).thenReturn(Nil)

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips shouldBe empty
  }

  it should "throw an exception for empty airport code" in {
    when(airportService.getAirportsByCode(List("")))
      .thenThrow(new NoSuchElementException("Airport with code not found"))

    an[NoSuchElementException] should be thrownBy {
      tripCreator.create("", date, numberOfDays)
    }
  }

  it should "handle flights with same departure and arrival times" in {
    when(airportService.getAirportByCode(londonGatwickFromCode)).thenReturn(londonGatwickAirport)
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    val outboundFlight = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 18, 10, 0),
      LocalDateTime.of(2025, 1, 18, 12, 0),
      100
    )

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight))
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight))

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips shouldBe empty
  }

  it should "create only one trip per destination with the cheapest price per hour option and sort trips by total price" in {
    val parisCharlesDeGaulleAirport =
      Airport("CDG", "Paris Charles de Gaulle", "CDG", "95565041", "France", 49.0097, 2.5479)
    val berlinTegelAirport          = Airport("TXL", "Berlin Tegel", "TXL", "95565042", "Germany", 52.5588, 13.2884)

    when(airportService.getAirportByCode(londonGatwickFromCode)).thenReturn(londonGatwickAirport)
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisCharlesDeGaulleAirport, berlinTegelAirport))

    val outboundFlight1 = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight1  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 18, 0),
      LocalDateTime.of(2025, 1, 20, 20, 0),
      100
    )
    val outboundFlight2 = Flight(
      "flight456",
      "SpeedyJet",
      "LGW",
      "TXL",
      LocalDateTime.of(2025, 1, 18, 9, 0),
      LocalDateTime.of(2025, 1, 18, 11, 0),
      150
    )
    val inboundFlight2  = Flight(
      "flight678",
      "AB Airlines",
      "TXL",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 19, 0),
      LocalDateTime.of(2025, 1, 20, 21, 0),
      150
    )
    val outboundFlight3 = Flight(
      "flight789",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 7, 0),
      LocalDateTime.of(2025, 1, 18, 9, 0),
      80
    )
    val inboundFlight3  = Flight(
      "flight910",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 17, 0),
      LocalDateTime.of(2025, 1, 20, 19, 0),
      80
    )

    when(flightService.getFlights(londonGatwickAirport, parisCharlesDeGaulleAirport, date))
      .thenReturn(List(outboundFlight1, outboundFlight3))
    when(flightService.getFlights(parisCharlesDeGaulleAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight1, inboundFlight3))
    when(flightService.getFlights(londonGatwickAirport, berlinTegelAirport, date)).thenReturn(List(outboundFlight2))
    when(flightService.getFlights(berlinTegelAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight2))

    val trips = tripCreator.create(londonGatwickFromCode, date, numberOfDays)

    trips should have size 2
    trips should contain(Trip("France", outboundFlight3, inboundFlight3))
    trips should contain(Trip("Germany", outboundFlight2, inboundFlight2))
    trips.head.totalPrice should be < trips(1).totalPrice
  }

  it should "create trips from a list of departure airports" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode, londonHeathrowFromCode)))
      .thenReturn(List(londonGatwickAirport, londonHeathrowAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    val outboundFlight1 = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight1  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LGW",
      LocalDateTime.of(2025, 1, 20, 18, 0),
      LocalDateTime.of(2025, 1, 20, 20, 0),
      100
    )
    val outboundFlight2 = Flight(
      "flight456",
      "SpeedyJet",
      "LHR",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 9, 0),
      LocalDateTime.of(2025, 1, 18, 11, 0),
      150
    )
    val inboundFlight2  = Flight(
      "flight678",
      "AB Airlines",
      "CDG",
      "LHR",
      LocalDateTime.of(2025, 1, 20, 19, 0),
      LocalDateTime.of(2025, 1, 20, 21, 0),
      150
    )

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight1))
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight1))
    when(flightService.getFlights(londonHeathrowAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight2))
    when(flightService.getFlights(parisOrlyAirport, londonHeathrowAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight2))

    val trips = tripCreator.create(List(londonGatwickFromCode, londonHeathrowFromCode), date, numberOfDays)

    trips should have size 1
    trips should contain(Trip("France", outboundFlight1, inboundFlight1))
  }

  it should "create trips that start at different airports if that is cheaper" in {
    when(airportService.getAirportsByCode(List(londonGatwickFromCode, londonHeathrowFromCode)))
      .thenReturn(List(londonGatwickAirport, londonHeathrowAirport))
    when(airportService.getAllAirportsInADifferentCountry(londonGatwickAirport.country))
      .thenReturn(List(parisOrlyAirport))

    val outboundFlight = Flight(
      "flight123",
      "SpeedyJet",
      "LGW",
      "CDG",
      LocalDateTime.of(2025, 1, 18, 8, 0),
      LocalDateTime.of(2025, 1, 18, 10, 0),
      100
    )
    val inboundFlight  = Flight(
      "flight345",
      "AB Airlines",
      "CDG",
      "LHR",
      LocalDateTime.of(2025, 1, 20, 18, 0),
      LocalDateTime.of(2025, 1, 20, 20, 0),
      80
    )

    when(flightService.getFlights(londonGatwickAirport, parisOrlyAirport, date)).thenReturn(List(outboundFlight))
    when(flightService.getFlights(londonHeathrowAirport, parisOrlyAirport, date)).thenReturn(Nil)
    when(flightService.getFlights(parisOrlyAirport, londonGatwickAirport, date.plusDays(numberOfDays)))
      .thenReturn(Nil)
    when(flightService.getFlights(parisOrlyAirport, londonHeathrowAirport, date.plusDays(numberOfDays)))
      .thenReturn(List(inboundFlight))

    val trips = tripCreator.create(List(londonGatwickFromCode, londonHeathrowFromCode), date, numberOfDays)

    trips should have size 1
    trips should contain(Trip("France", outboundFlight, inboundFlight))
  }
}
