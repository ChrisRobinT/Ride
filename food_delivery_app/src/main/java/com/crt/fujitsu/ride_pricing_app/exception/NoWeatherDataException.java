package com.crt.fujitsu.ride_pricing_app.exception;

public class NoWeatherDataException extends RuntimeException {
    public NoWeatherDataException(String message) {
        super(message);
    }
}
