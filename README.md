# Ride Price Calculator

A Spring Boot application that calculates ride-sharing prices for Estonian cities (Tallinn, Tartu, Pärnu) based on vehicle type, live weather data, and time of day.

## Features

- **Dynamic pricing** — Base fees vary by city and vehicle (Car, Bike, Scooter)
- **Weather-based extras** — Temperature, wind speed, and weather phenomena affect scooter and bike fares
- **Time-of-day multipliers** — Rush hour (07:00–09:00, 16:00–19:00) and late night (00:00–05:00) adjustments
- **Risk score** — 0–100 score indicating ride conditions based on weather
- **Live weather data** — Fetches current conditions from WeatherAPI.com every 45 seconds

## Tech Stack

- **Java 21** · **Spring Boot 3.4** · **Spring Data JPA** · **H2 Database**
- **WeatherAPI.com** for real-time weather
- **SpringDoc OpenAPI** for API documentation

## Prerequisites

- Java 21 or later
- Maven 3.6+ (or use the included Maven wrapper)

## Getting Started

### Run the application

```bash
cd food_delivery_app
./mvnw spring-boot:run
```

On Windows:

```bash
cd food_delivery_app
mvnw.cmd spring-boot:run
```

### Access the app

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Web UI — price calculator |
| http://localhost:8080/ride-price | REST API endpoint |
| http://localhost:8080/swagger-ui.html | OpenAPI / Swagger docs |
| http://localhost:8080/h2-console | H2 database console |

> **Note:** Weather data is imported every 45 seconds. Wait ~1 minute after startup before requesting prices.

## API

### `GET /ride-price`

Calculate ride price for a given city, vehicle, and optional ride time.

**Query parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | Yes | `Tallinn`, `Tartu`, or `Pärnu` |
| `vehicle` | string | Yes | `Car`, `Bike`, or `Scooter` |
| `rideTime` | string | No | ISO 8601 datetime (e.g. `2026-02-04T09:37:00.000Z`). Defaults to current time. |

**Example request**

```
GET /ride-price?city=Tallinn&vehicle=Car&rideTime=2026-02-04T09:37:00.000Z
```

**Example response**

```json
{
  "baseFee": 4.0,
  "extraFee": 0.0,
  "multiplier": 1.2,
  "finalPrice": 4.8,
  "riskScore": 85
}
```

## Pricing Rules

### Base fees (€)

| City | Car | Scooter | Bike |
|------|-----|---------|------|
| Tallinn | 4.00 | 3.50 | 3.00 |
| Tartu | 3.50 | 3.00 | 2.50 |
| Pärnu | 3.00 | 2.50 | 2.00 |

### Extra fees (Scooter & Bike only)

- **Temperature:** Below -10°C: +€1.00 · -10°C to 0°C: +€0.50
- **Wind (Bike):** 10–20 m/s: +€0.50 · Above 20 m/s: forbidden
- **Weather:** Snow/sleet: +€1.00 · Rain: +€0.50 · Glaze/hail/thunder: forbidden

### Time multipliers

- **07:00–09:00, 16:00–19:00** → ×1.2
- **00:00–05:00** → ×1.1
- **Other times** → ×1.0

## Project Structure

```
food_delivery_app/
├── src/main/java/com/crt/fujitsu/ride_pricing_app/
│   ├── config/          # RestTemplate, CORS
│   ├── controller/      # REST & web controllers
│   ├── dto/             # RidePriceEstimate
│   ├── exception/       # Error handling
│   ├── model/           # WeatherData entity
│   ├── repository/      # WeatherDataRepository
│   └── service_logic/   # Pricing, weather, calculators
├── src/main/resources/
│   ├── application.properties
│   └── static/index.html
└── pom.xml
```

## Configuration

- **Database:** H2 in-memory (`jdbc:h2:mem:weather_database`)
- **Weather API:** WeatherAPI.com (key in `ApiWeather.java`)
- **Weather import:** Every 45 seconds via `@Scheduled`

## Running Tests

```bash
cd food_delivery_app
./mvnw test
```

## License

MIT
