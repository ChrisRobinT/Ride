package com.crt.fujitsu.ride_pricing_app.service_logic;

import com.crt.fujitsu.ride_pricing_app.model.WeatherData;

public class ExtraFeeCalculator {
    public static double getExtraFee(String vehicle, WeatherData weatherData) {
        double extraFee = 0.0;
        if (vehicle.equalsIgnoreCase("Scooter") || vehicle.equalsIgnoreCase("Bike")) {
            if (isSevereWeather(weatherData)) {
                throw new IllegalStateException("Usage of selected vehicle type is forbidden");
            }
            extraFee += getTempFee(weatherData.getAirTemperature());
            extraFee += getPhenomenonFee(weatherData.getPhenomenon());
        }
        if (vehicle.equalsIgnoreCase("Bike") && weatherData.getWindSpeed() != null) {
            extraFee += getWindSpeedFee(weatherData.getWindSpeed());
        }
        return extraFee;
    }

    private static boolean isSevereWeather(WeatherData weatherData) {
        if (weatherData.getPhenomenon() != null) {
            String phenomenon = weatherData.getPhenomenon().toLowerCase();
            return phenomenon.contains("glaze") || phenomenon.contains("thunder") || phenomenon.contains("hail");
        }
        return false;
    }

    private static double getWindSpeedFee(Double windSpeed) {
        if (windSpeed > 20.0)
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        else if (windSpeed >= 10.0) return 0.5;
        return 0.0;
    }

    private static double getTempFee(Double temp) {
        if (temp == null) return 0.0;
        if (temp < -10) return 1.0;
        else if (temp >= -10 && temp < 0) return 0.5;
        return 0.0;
    }

    private static double getPhenomenonFee(String phenomenon) {
        if (phenomenon == null) return 0.0;
        phenomenon = phenomenon.toLowerCase();
        if (phenomenon.contains("snow") || phenomenon.contains("sleet")) return 1.0;
        else if (phenomenon.contains("rain")) return 0.5;
        return 0.0;
    }
}
