# Flight Backend

A Scala Play Framework application that provides flight and trip recommendations based on various parameters such as departure airports, dates, and duration.

## Table of Contents
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [API Specification](#api-specification)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)

## Getting Started

### Prerequisites
- Java 21.0.2 or higher
- SBT 1.10.6 or higher
- Scala 2.13.15

### Installation
1. Clone the repository
```bash
git clone [repository-url]
cd flight-backend
```

2. Install dependencies
```bash
sbt update
```

## Running the Application
To run the application locally:
```bash
sbt run
```
The application will start on `http://localhost:9000` by default.

## Running Tests
To run the test suite:
```bash
sbt test
```

## API Specification

### Base URL
`/api`

### Endpoints

#### 1. Get Specific Date Trips
Retrieves trip recommendations for specific dates.

**Endpoint:** `GET /trips`

##### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| fromCode | string | Yes | Comma-separated list of departure airport IATA codes (e.g., "LHR" or "LHR,LGW") |
| date | string | Yes | Departure date in ISO_LOCAL_DATE format (YYYY-MM-DD) |
| numberOfDays | integer | Yes | Duration of the trip in days |

##### Example Requests

1. Single departure airport:
```
GET /api/trips?fromCode=LHR&date=2025-01-18&numberOfDays=2
```

2. Multiple departure airports:
```
GET /api/trips?fromCode=LHR,LGW&date=2025-01-18&numberOfDays=2
```

#### 2. Get Weekend Trips
Retrieves trip recommendations for weekends in a specific month.

**Endpoint:** `GET /weekends`

##### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| fromCode | string | Yes | Comma-separated list of departure airport IATA codes (e.g., "LHR" or "LHR,LGW") |
| month | integer | Yes | Month number (1-12) |
| year | integer | Yes | Year (e.g., 2024) |
| numberOfExtraDays | integer | Yes | Number of additional days to add to the weekend (0-2) |

##### Example Requests

1. Basic weekend trips:
```
GET /api/weekends?fromCode=LHR&month=6&year=2024&numberOfExtraDays=0
```

2. Extended weekend with multiple airports:
```
GET /api/weekends?fromCode=LHR,LGW&month=6&year=2024&numberOfExtraDays=1
```

#### Common Response Format
Both endpoints return the same response format:

```json
[
  {
    "destination": "string",      // Destination country (e.g., "France")
    "outboundFlight": {
      "flightNumber": "string",   // Flight number (e.g., "FL123")
      "airline": "string",        // Airline name
      "from": {
        "code": "string",         // IATA code
        "name": "string",         // Airport name
        "country": "string",      // Country code
        "skyId": "string",        // Sky ID
        "entityId": "string"      // Entity ID
      },
      "to": {
        // Same structure as "from"
      },
      "departureTime": "string",  // ISO DateTime
      "arrivalTime": "string",    // ISO DateTime
      "price": number            // Flight price
    },
    "inboundFlight": {
      // Same structure as outboundFlight
    }
  }
]
```

#### Response Codes
| Status Code | Description |
|-------------|-------------|
| 200 | Success. Returns array of trip recommendations |
| 400 | Bad Request. Common error cases: |
|     | - Missing required parameters |
|     | - Invalid date format (must be YYYY-MM-DD) |
|     | - Invalid month (must be 1-12) |
|     | - Invalid number of extra days (must be 0-2) |
|     | - Invalid number format |

##### Example Response
```json
[
  {
    "destination": "France",
    "outboundFlight": {
      "flightNumber": "FL123",
      "airline": "British Airways",
      "from": {
        "code": "LHR",
        "name": "London Heathrow",
        "country": "GB",
        "skyId": "12345",
        "entityId": "67890"
      },
      "to": {
        "code": "CDG",
        "name": "Paris Charles de Gaulle",
        "country": "FR",
        "skyId": "54321",
        "entityId": "09876"
      },
      "departureTime": "2025-01-18T10:00:00",
      "arrivalTime": "2025-01-18T12:00:00",
      "price": 100.0
    },
    "inboundFlight": {
      "flightNumber": "FL124",
      "airline": "Air France",
      "from": {
        "code": "CDG",
        "name": "Paris Charles de Gaulle",
        "country": "FR",
        "skyId": "54321",
        "entityId": "09876"
      },
      "to": {
        "code": "LHR",
        "name": "London Heathrow",
        "country": "GB",
        "skyId": "12345",
        "entityId": "67890"
      },
      "departureTime": "2025-01-20T14:00:00",
      "arrivalTime": "2025-01-20T16:00:00",
      "price": 120.0
    }
  }
]
```

### Notes
- The API uses caching to improve performance
- Empty `fromCode` will return an empty array of trips
- All times are in ISO DateTime format
- Prices are in the default currency unit (assumed to be in the same currency throughout)
- Weekend trips are automatically calculated for all weekends in the specified month
- For weekend trips, the best (cheapest) options are returned for each destination
- The API requires proper configuration including cache settings

## Project Structure
```
flight-backend/
├── app/
│   ├── controllers/
│   │   └── ApiController.scala
│   │   ├── services/
│   │   │   ├── ApiService.scala
│   │   │   ├── HttpApiService.scala
│   │   │   ├── FlightService.scala
│   │   │   ├── AirportService.scala
│   │   │   └── TripCreator.scala
│   │   ├── models/
│   │   │   ├── Airport.scala
│   │   │   ├── Flight.scala
│   │   │   └── Trip.scala
│   │   └── cache/
│   │       └── CachingApiService.scala
│   ├── test/
│   │   ├── controllers/
│   │   │   └── ApiControllerSpec.scala
│   │   └── services/
│   │       ├── HttpApiServiceSpec.scala
│   │       ├── FlightServiceSpec.scala
│   │       ├── AirportServiceSpec.scala
│   │       └── TripCreatorSpec.scala
│   ├── conf/
│   │   ├── application.conf
│   │   └── routes
│   ├── build.sbt
│   └── README.md
```

## Dependencies
- Play Framework 3.0.6
- Scala Test 3.2.10
- ScalaJ HTTP 2.4.2
- Mockito Scala 1.17.30
- Guava 32.0.1-jre

For the complete list of dependencies and their versions, please refer to the `build.sbt` file. 