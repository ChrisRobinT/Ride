package com.crt.fujitsu.ride_pricing_app;

import com.crt.fujitsu.ride_pricing_app.exception.NoWeatherDataException;
import com.crt.fujitsu.ride_pricing_app.service_logic.ApiWeather;
import com.crt.fujitsu.ride_pricing_app.service_logic.PriceMultiplierCalculator;
import com.crt.fujitsu.ride_pricing_app.service_logic.RiskScoreCalculator;
import com.crt.fujitsu.ride_pricing_app.service_logic.PricingService;
import com.crt.fujitsu.ride_pricing_app.dto.RidePriceEstimate;
import com.crt.fujitsu.ride_pricing_app.model.WeatherData;
import com.crt.fujitsu.ride_pricing_app.repository.WeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_XML;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.task.scheduling.enabled=false"})
class RidePricingAppApplicationTests {

	@Autowired
	private WeatherDataRepository weatherDataRepository;

	@BeforeEach
	void clear() {
		weatherDataRepository.deleteAll();
	}

	// === PricingService Unit Tests ===

	@Test
	void testCalculateRidePrice_TallinnAndCar() {
		// Given
		String city = "Tallinn";
		String vehicle = "Car";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn");
		testWeatherData.setAirTemperature(5.0);
		testWeatherData.setWindSpeed(5.0);
		testWeatherData.setPhenomenon("clear");
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);

		// When – use a fixed rideTime (noon, multiplier=1.0)
		PricingService pricingService = new PricingService(weatherDataRepository);
		RidePriceEstimate estimate = pricingService.calculateRidePrice(city, vehicle, LocalDateTime.of(2025, 4, 5, 12, 0));

		// Then
		double expectedFee = 4.0;
		double actualFee = estimate.getFinalPrice();
		assertEquals(expectedFee, actualFee, 0.001, "Fee should match expected");
	}

	@Test
	void testCalculateRidePrice_NoCitySpecified() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		String vehicle = "Car";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(null, vehicle, LocalDateTime.now()));
		assertEquals("No city specified", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_NoVehicleSpecified() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		String city = "Tallinn";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(city, null, LocalDateTime.now()));
		assertEquals("No vehicle type specified", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_InvalidVehicle() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		String city = "Tallinn";
		String vehicle = "Submarine";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("Invalid vehicle type: " + vehicle, exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_UnknownCity() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		String city = "Tõrva";
		String vehicle = "Car";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("Unknown city: " + city, exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_WeatherDataNotAvailable() {
		String city = "Tallinn";
		String vehicle = "Car";
		PricingService pricingService = new PricingService(weatherDataRepository);
		NoWeatherDataException exception = assertThrows(NoWeatherDataException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("No weather data available for city: " + city, exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_RestrictedDueToWeather() {
		String city = "Tallinn";
		String vehicle = "Bike";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn");
		testWeatherData.setAirTemperature(5.0);
		testWeatherData.setWindSpeed(20.0001);  // Too high wind speed
		testWeatherData.setPhenomenon("Clear");
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);
		PricingService pricingService = new PricingService(weatherDataRepository);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_RestrictedDueToPhenomenon() {
		String city = "Tallinn";
		String vehicle = "Scooter";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn");
		testWeatherData.setAirTemperature(5.0);
		testWeatherData.setWindSpeed(5.0);
		testWeatherData.setPhenomenon("Glaze");
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);
		PricingService pricingService = new PricingService(weatherDataRepository);
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_ExtraFeeCalculation() {
		String city = "Tartu";
		String vehicle = "Scooter";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tartu");
		testWeatherData.setAirTemperature(-5.0);  // triggers extra fee
		testWeatherData.setWindSpeed(15.0);        // moderate wind
		testWeatherData.setPhenomenon("snow");     // triggers extra fee
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);
		double baseFee = 3.0;
		double extraFee = 1.5;
		double expectedFee = baseFee + extraFee;
		PricingService pricingService = new PricingService(weatherDataRepository);
		RidePriceEstimate estimate = pricingService.calculateRidePrice(city, vehicle, LocalDateTime.of(2025, 4, 5, 12, 0));
		double actualFee = estimate.getFinalPrice();
		assertEquals(expectedFee, actualFee, 0.001, "The ride price should match the expected value.");
	}

	@Test
	void testCalculateRidePrice_NullCityAndVehicle() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice(null, null, LocalDateTime.now()));
		assertEquals("No city specified", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_EmptyCityAndVehicle() {
		PricingService pricingService = new PricingService(weatherDataRepository);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> pricingService.calculateRidePrice("", "", LocalDateTime.now()));
		assertEquals("No city specified", exception.getMessage());
	}

	@Test
	void testCalculateRidePrice_MaxWindSpeedBike() {
		String city = "Tallinn";
		String vehicle = "Bike";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn");
		testWeatherData.setAirTemperature(10.0);
		testWeatherData.setWindSpeed(20.0);  // Maximum acceptable wind speed for bike
		testWeatherData.setPhenomenon("clear");
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);
		double expectedFee = 3.0 + 0.5;  // baseFee (3.0) + extraFee (0.5)
		PricingService pricingService = new PricingService(weatherDataRepository);
		RidePriceEstimate estimate = pricingService.calculateRidePrice(city, vehicle, LocalDateTime.of(2025, 4, 5, 12, 0));
		double actualFee = estimate.getFinalPrice();
		assertEquals(expectedFee, actualFee, 0.001, "Final price should match expected fee.");
	}

	@Test
	void testCalculateRidePrice_MissingPhenomenon() {
		String city = "Tallinn";
		String vehicle = "Scooter";
		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn");
		testWeatherData.setAirTemperature(0.0);
		testWeatherData.setWindSpeed(5.0);
		testWeatherData.setPhenomenon(null);  // Missing phenomenon
		testWeatherData.setObservationTimestamp(LocalDateTime.now());
		weatherDataRepository.save(testWeatherData);
		double expectedFee = 3.5;
		PricingService pricingService = new PricingService(weatherDataRepository);
		RidePriceEstimate estimate = pricingService.calculateRidePrice(city, vehicle, LocalDateTime.of(2025, 4, 5, 12, 0));
		double actualFee = estimate.getFinalPrice();
		assertEquals(expectedFee, actualFee, 0.001, "Final price should match expected fee for missing phenomenon.");
	}

	@Test
	void testCalculateRidePrice_EmptyRepository() {
		String city = "Tallinn";
		String vehicle = "Car";
		PricingService pricingService = new PricingService(weatherDataRepository);
		NoWeatherDataException exception = assertThrows(NoWeatherDataException.class,
				() -> pricingService.calculateRidePrice(city, vehicle, LocalDateTime.now()));
		assertEquals("No weather data available for city: " + city, exception.getMessage());
	}

	// === Price Multiplier Calculator Tests ===
	@Nested
	@SpringBootTest
	public class PriceMultiplierCalculatorTests {

		@Test
		void testMorningRushHour() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 8, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.2, multiplier, 0.001, "Morning rush hour (8:00) should have multiplier 1.2");
		}

		@Test
		void testEveningRushHour() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 17, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.2, multiplier, 0.001, "Evening rush hour (17:00) should have multiplier 1.2");
		}

		@Test
		void testEarlyMorning() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 3, 30);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.1, multiplier, 0.001, "Early morning (3:30) should have multiplier 1.1");
		}

		@Test
		void testNormalTime() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 12, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.0, multiplier, 0.001, "Normal time (12:00) should have multiplier 1.0");
		}

		@Test
		void testEdgeCaseAt6AM() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 6, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.0, multiplier, 0.001, "At exactly 6:00 AM, multiplier should be 1.0");
		}

		@Test
		void testEdgeCaseAt10AM() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 10, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.0, multiplier, 0.001, "At exactly 10:00 AM, multiplier should be 1.0");
		}

		@Test
		void testEdgeCaseAt3PM() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 15, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.0, multiplier, 0.001, "At exactly 15:00, multiplier should be 1.0");
		}

		@Test
		void testEdgeCaseAt8PM() {
			LocalDateTime rideTime = LocalDateTime.of(2025, 4, 5, 20, 0);
			double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
			assertEquals(1.0, multiplier, 0.001, "At exactly 20:00, multiplier should be 1.0");
		}
	}

	// === Risk Score Calculator Tests ===
	@Nested
	@SpringBootTest
	class RiskScoreCalculatorTests {

		@Test
		void testNormalCarNoDeductions() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(5.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("clear");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Car");
			assertEquals(100, score, "For a Car with normal weather, score should be 100.");
		}

		@Test
		void testModerateWindBike() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(15.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("clear");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Bike");
			assertEquals(85, score, "For a Bike with moderate wind, score should be 85.");
		}

		@Test
		void testTemperatureOutOfRangeForScooter() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(5.0);
			weatherData.setAirTemperature(-15.0);
			weatherData.setPhenomenon("clear");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Scooter");
			assertEquals(65, score, "For a Scooter with low temperature, score should be 65.");
		}

		@Test
		void testPhenomenonModerateCar() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(5.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("rain");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Car");
			assertEquals(95, score, "For a Car with rain, score should be 95.");
		}

		@Test
		void testPhenomenonSevereBike() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(5.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("thunder");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Bike");
			assertEquals(70, score, "For a Bike with thunder, score should be 70.");
		}

		@Test
		void testSevereWindScooter() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(22.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("clear");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Scooter");
			assertEquals(60, score, "For a Scooter with severe wind, score should be 60.");
		}

		@Test
		void testClampingScoreLowerBound() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(100.0);
			weatherData.setAirTemperature(-50.0);
			weatherData.setPhenomenon("thunder");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Scooter");
			assertEquals(0, score, "Score should be clamped to 0 if deductions exceed 100.");
		}

		@Test
		void testClampingScoreUpperBound() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(0.0);
			weatherData.setAirTemperature(25.0);
			weatherData.setPhenomenon("clear");

			int score = RiskScoreCalculator.computeRiskScore(weatherData, "Car");
			assertEquals(100, score, "Score should remain 100 if no deductions apply.");
		}

		@Test
		void testInvalidVehicle() {
			WeatherData weatherData = new WeatherData();
			weatherData.setWindSpeed(10.0);
			weatherData.setAirTemperature(20.0);
			weatherData.setPhenomenon("clear");

			Exception exception = assertThrows(IllegalArgumentException.class,
					() -> RiskScoreCalculator.computeRiskScore(weatherData, "Submarine"));
			assertTrue(exception.getMessage().contains("SUBMARINE"),
					"Exception should mention the invalid vehicle type 'SUBMARINE'");
		}
	}

	// === Pricing Controller Integration Tests ===
	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			properties = {"spring.task.scheduling.enabled=false"})
	public class PricingControllerIntegrationTest {

		@Autowired
		private TestRestTemplate restTemplate;

		@Autowired
		private WeatherDataRepository weatherDataRepository;

		@BeforeEach
		void setUp() {
			weatherDataRepository.deleteAll();
		}

		@Test
		void testGetRidePriceEndpoint_TallinnAndCar() {
			WeatherData testData = new WeatherData();
			testData.setStationName("Tallinn");
			testData.setAirTemperature(5.0);
			testData.setWindSpeed(5.0);
			testData.setPhenomenon("clear");
			testData.setObservationTimestamp(LocalDateTime.now());
			weatherDataRepository.save(testData);

			// Provide rideTime explicitly so that the multiplier is 1.0
			String url = "/ride-price?city=Tallinn&vehicle=Car&rideTime=2025-04-05T12:00:00";
			ResponseEntity<RidePriceEstimate> response = restTemplate.getForEntity(url, RidePriceEstimate.class);

			assertEquals(HttpStatus.OK, response.getStatusCode());
			RidePriceEstimate estimate = response.getBody();
			assertNotNull(estimate);
			// Expected fee for a Car in Tallinn with these normal conditions
			assertEquals(4.0, estimate.getFinalPrice(), 0.001, "Final price should match expected fee.");
		}

		@Test
		void testGetRidePriceEndpoint_NoWeatherData() {
			String url = "/ride-price?city=Tallinn&vehicle=Car&rideTime=2025-04-05T12:00:00";
			// With an empty repository, the PricingService should throw an exception and GlobalExceptionHandler returns 400.
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			// Expect error status 400 BAD_REQUEST.
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
					"Expected 400 BAD_REQUEST when no weather data available.");
			assertTrue(response.getBody().contains("No weather data available for city: Tallinn"));
		}
	}


	// === ApiWeather Integration Tests (with MockRestServiceServer) ===
	@Nested
	@SpringBootTest(properties = {"spring.task.scheduling.enabled=false"})
	public class ApiWeatherIntegrationTest {

		@Autowired
		private WeatherDataRepository weatherDataRepository;

		@Autowired
		private ApiWeather apiWeather;

		@Autowired
		private RestTemplate restTemplate;

		private MockRestServiceServer mockServer;

		@BeforeEach
		void setup() {
			weatherDataRepository.deleteAll();
			mockServer = MockRestServiceServer.createServer(restTemplate);
			ReflectionTestUtils.setField(apiWeather, "restTemplate", restTemplate);
		}

		@Test
		void testImportWeatherData_success() throws Exception {
			String tallinnResponse = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
            <location>
                <name>Tallinn</name>
                <country>Estonia</country>
            </location>
            <current>
                <temp_c>5.0</temp_c>
                <wind_kph>5.0</wind_kph>
                <condition>
                    <text>clear</text>
                </condition>
            </current>
        </root>
        """;

			String tartuResponse = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
            <location>
                <name>Tartu</name>
                <country>Estonia</country>
            </location>
            <current>
                <temp_c>4.0</temp_c>
                <wind_kph>3.0</wind_kph>
                <condition>
                    <text>partly cloudy</text>
                </condition>
            </current>
        </root>
        """;

			String parnuResponse = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
            <location>
                <name>Pärnu</name>
                <country>Estonia</country>
            </location>
            <current>
                <temp_c>6.0</temp_c>
                <wind_kph>7.0</wind_kph>
                <condition>
                    <text>rain</text>
                </condition>
            </current>
        </root>
        """;

			// Mock response for Tallinn
			mockServer.expect(ExpectedCount.once(),
							requestTo(allOf(
									containsString("http://api.weatherapi.com/v1/current.xml"),
									containsString("key=20ac311827124d0ba50125028250404"),
									containsString("q=Tallinn"),
									containsString("aqi=no")
							)))
					.andExpect(method(GET))
					.andRespond(withSuccess(tallinnResponse, APPLICATION_XML));

			// Mock response for Tartu_Estonia
			mockServer.expect(ExpectedCount.once(),
							requestTo(allOf(
									containsString("http://api.weatherapi.com/v1/current.xml"),
									containsString("key=20ac311827124d0ba50125028250404"),
									containsString("q=Tartu_Estonia"),
									containsString("aqi=no")
							)))
					.andExpect(method(GET))
					.andRespond(withSuccess(tartuResponse, APPLICATION_XML));

			// Mock response for Pärnu - using the URL-encoded value for the test
			mockServer.expect(ExpectedCount.once(),
							requestTo(allOf(
									containsString("http://api.weatherapi.com/v1/current.xml"),
									containsString("key=20ac311827124d0ba50125028250404"),
									containsString("q=P%C3%A4rnu"),
									containsString("aqi=no")
							)))
					.andExpect(method(GET))
					.andRespond(withSuccess(parnuResponse, APPLICATION_XML));

			apiWeather.importWeatherData();

			List<WeatherData> savedData = weatherDataRepository.findAll();
			assertFalse(savedData.isEmpty(), "Weather data should have been saved");
			assertEquals(3, savedData.size(), "Three weather entries should be saved");

			// Verify the first entry
			WeatherData tallinnData = savedData.stream()
					.filter(data -> data.getStationName().equals("Tallinn"))
					.findFirst()
					.orElseThrow();
			assertEquals(5.0, tallinnData.getAirTemperature(), 0.001);

			mockServer.verify();
		}
	}
}
