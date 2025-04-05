package com.crt.fujitsu.ride_pricing_app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_database")
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stationName;
    private String wmoCode;
    private Double airTemperature;
    private Double windSpeed;
    private String phenomenon;
    private LocalDateTime observationTimestamp;

    public WeatherData() {
    }

    // Parameterized constructor
    public WeatherData(String stationName, String wmoCode, Double airTemperature,
                       Double windSpeed, String phenomenon, LocalDateTime observationTimestamp) {
        this.stationName = stationName;
        this.wmoCode = wmoCode;
        this.airTemperature = airTemperature;
        this.windSpeed = windSpeed;
        this.phenomenon = phenomenon;
        this.observationTimestamp = observationTimestamp;
    }

    // Getters and Setters
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationName() {
        return stationName;
    }

    public void setWmoCode(String wmoCode) {
        this.wmoCode = wmoCode;
    }

    public String getWmoCode() {
        return wmoCode;
    }

    public void setAirTemperature(Double airTemperature) {
        this.airTemperature = airTemperature;
    }

    public Double getAirTemperature() {
        return airTemperature;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setPhenomenon(String phenomenon) {
        this.phenomenon = phenomenon;
    }

    public String getPhenomenon() {
        return phenomenon;
    }

    public void setObservationTimestamp(LocalDateTime observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public LocalDateTime getObservationTimestamp() {
        return observationTimestamp;
    }

    @Override
    public String toString() {
        return "weatherData{" +
                "id=" + id +
                ", stationName='" + stationName + '\'' +
                ", wmoCode='" + wmoCode + '\'' +
                ", airTemperature=" + airTemperature +
                ", windSpeed=" + windSpeed +
                ", phenomenon='" + phenomenon + '\'' +
                ", observationTimestamp=" + observationTimestamp +
                '}';
    }
}
