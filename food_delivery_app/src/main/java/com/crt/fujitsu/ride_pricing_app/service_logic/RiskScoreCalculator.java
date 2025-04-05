package com.crt.fujitsu.ride_pricing_app.service_logic;

import com.crt.fujitsu.ride_pricing_app.model.WeatherData;

import java.util.Map;

public class RiskScoreCalculator {
    private static final int BASE_SCORE = 100;
    private static final double HIGH_WIND_THRESHOLD = 20.0;
    private static final double MODERATE_WIND_THRESHOLD = 10.0;
    private static final double LOW_TEMP_THRESHOLD = -10.0;
    private static final double HIGH_TEMP_THRESHOLD = 35.0;

    private enum VehicleType {
        SCOOTER, BIKE, CAR;

        static VehicleType fromString(String vehicle) {
            if (vehicle == null) throw new IllegalArgumentException("Vehicle type cannot be null");
            return valueOf(vehicle.toUpperCase());
        }
    }

    private enum WeatherSeverity {
        SEVERE, MODERATE, MILD
    }

    public static int computeRiskScore(WeatherData weatherData, String vehicleStr) {
        int score = BASE_SCORE;
        VehicleType vehicle = VehicleType.fromString(vehicleStr);

        // Apply wind deduction
        score -= calculateWindDeduction(weatherData, vehicle);

        // Apply temperature deduction
        score -= calculateTemperatureDeduction(weatherData, vehicle);

        // Apply weather phenomenon deduction
        score -= calculatePhenomenonDeduction(weatherData, vehicle);

        // Apply vehicle-specific base deduction
        score -= getVehicleBaseDeduction(vehicle);

        // Ensure the final score is between 0 and 100
        return Math.max(0, Math.min(100, score));
    }

    private static int calculateWindDeduction(WeatherData weatherData, VehicleType vehicle) {
        double windSpeed = weatherData.getWindSpeed() != null ? weatherData.getWindSpeed() : 0.0;

        if (windSpeed <= MODERATE_WIND_THRESHOLD) {
            return 0;
        }

        WeatherSeverity severity;
        if (windSpeed > HIGH_WIND_THRESHOLD) {
            severity = WeatherSeverity.SEVERE;
        } else {
            severity = WeatherSeverity.MODERATE;
        }

        Map<VehicleType, Map<WeatherSeverity, Integer>> deductions = Map.of(
                VehicleType.SCOOTER, Map.of(WeatherSeverity.SEVERE, 30, WeatherSeverity.MODERATE, 15),
                VehicleType.BIKE, Map.of(WeatherSeverity.SEVERE, 20, WeatherSeverity.MODERATE, 10),
                VehicleType.CAR, Map.of(WeatherSeverity.SEVERE, 10, WeatherSeverity.MODERATE, 5)
        );

        return deductions.getOrDefault(vehicle, Map.of())
                .getOrDefault(severity, 0);
    }

    private static int calculateTemperatureDeduction(WeatherData weatherData, VehicleType vehicle) {
        Double temperature = weatherData.getAirTemperature();
        if (temperature == null || (temperature >= LOW_TEMP_THRESHOLD && temperature <= HIGH_TEMP_THRESHOLD)) {
            return 0;
        }

        Map<VehicleType, Integer> deductions = Map.of(
                VehicleType.SCOOTER, 25,
                VehicleType.BIKE, 15,
                VehicleType.CAR, 10
        );

        return deductions.getOrDefault(vehicle, 0);
    }

    private static int calculatePhenomenonDeduction(WeatherData weatherData, VehicleType vehicle) {
        String phenomenon = weatherData.getPhenomenon() != null ?
                weatherData.getPhenomenon().toLowerCase() : "";

        if (phenomenon.isEmpty()) {
            return 0;
        }

        WeatherSeverity severity = determinePhenomenonSeverity(phenomenon);
        if (severity == WeatherSeverity.MILD) {
            return 0;
        }

        Map<VehicleType, Map<WeatherSeverity, Integer>> deductions = Map.of(
                VehicleType.SCOOTER, Map.of(WeatherSeverity.SEVERE, 35, WeatherSeverity.MODERATE, 15),
                VehicleType.BIKE, Map.of(WeatherSeverity.SEVERE, 25, WeatherSeverity.MODERATE, 10),
                VehicleType.CAR, Map.of(WeatherSeverity.SEVERE, 15, WeatherSeverity.MODERATE, 5)
        );

        return deductions.getOrDefault(vehicle, Map.of())
                .getOrDefault(severity, 0);
    }

    private static WeatherSeverity determinePhenomenonSeverity(String phenomenon) {
        if (phenomenon.contains("thunder") || phenomenon.contains("hail") || phenomenon.contains("glaze")) {
            return WeatherSeverity.SEVERE;
        } else if (phenomenon.contains("rain") || phenomenon.contains("snow")) {
            return WeatherSeverity.MODERATE;
        }
        return WeatherSeverity.MILD;
    }

    private static int getVehicleBaseDeduction(VehicleType vehicle) {
        Map<VehicleType, Integer> baseDeductions = Map.of(
                VehicleType.SCOOTER, 10,
                VehicleType.BIKE, 5,
                VehicleType.CAR, 0
        );

        return baseDeductions.getOrDefault(vehicle, 0);
    }

}
