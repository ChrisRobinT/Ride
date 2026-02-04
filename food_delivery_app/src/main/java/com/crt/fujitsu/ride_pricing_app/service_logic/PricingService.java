package com.crt.fujitsu.ride_pricing_app.service_logic;

import com.crt.fujitsu.ride_pricing_app.controller.PricingController;
import com.crt.fujitsu.ride_pricing_app.dto.RidePriceEstimate;
import com.crt.fujitsu.ride_pricing_app.exception.NoWeatherDataException;
import com.crt.fujitsu.ride_pricing_app.model.WeatherData;
import com.crt.fujitsu.ride_pricing_app.repository.WeatherDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PricingService {
    private final WeatherDataRepository weatherDataRepository;
    private final Map<String, String> cityToStationMap;
    private final List<String> validVehicles = Arrays.asList("Car", "Bike", "Scooter");

    public PricingService(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
        cityToStationMap = new HashMap<>();
        cityToStationMap.put("Tallinn", "Tallinn");
        cityToStationMap.put("Tartu", "Tartu");
        cityToStationMap.put("Pärnu", "Pärnu");
    }


    public RidePriceEstimate calculateRidePrice(String city, String vehicle, LocalDateTime rideTime) {
        // Validate input parameters first
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("No city specified");
        }
        if (vehicle == null || vehicle.trim().isEmpty()) {
            throw new IllegalArgumentException("No vehicle type specified");
        }
        if (!validVehicles.contains(vehicle)) {
            throw new IllegalArgumentException("Invalid vehicle type: " + vehicle);
        }

        String stationName = cityToStationMap.get(city);
        if (stationName == null) {
            throw new IllegalArgumentException("Unknown city: " + city);
        }

        // Fetch weather data (station names are canonical: Tallinn, Tartu, Pärnu)
        WeatherData weatherData = weatherDataRepository.findTopByStationNameOrderByObservationTimestampDesc(stationName)
                .orElseThrow(() -> new NoWeatherDataException("No weather data available for city: " + city));

        // Calculate pricing
        double baseFee = BaseFeeCalculator.getBaseFee(city, vehicle);
        double extraFee = ExtraFeeCalculator.getExtraFee(vehicle, weatherData);
        int riskScore = RiskScoreCalculator.computeRiskScore(weatherData, vehicle);
        double multiplier = PriceMultiplierCalculator.calculatePriceMultiplier(rideTime);
        double finalPrice = (baseFee + extraFee) * multiplier;

        return new RidePriceEstimate(baseFee, extraFee, multiplier, finalPrice, riskScore);
    }
}