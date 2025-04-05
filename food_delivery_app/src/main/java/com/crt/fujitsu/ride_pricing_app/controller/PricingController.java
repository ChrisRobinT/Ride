package com.crt.fujitsu.ride_pricing_app.controller;

import com.crt.fujitsu.ride_pricing_app.dto.RidePriceEstimate;
import com.crt.fujitsu.ride_pricing_app.service_logic.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/ride-price")
    public ResponseEntity<RidePriceEstimate> getRidePrice(
            @RequestParam String city,
            @RequestParam String vehicle,
            @RequestParam(required = false) String rideTime) {

        // Parse rideTime if provided, otherwise use current time.
        LocalDateTime parsedRideTime = (rideTime != null && !rideTime.isEmpty())
                ? LocalDateTime.parse(rideTime)
                : LocalDateTime.now();

        // Let the service throw an exception if no weather data exists.
        RidePriceEstimate estimate = pricingService.calculateRidePrice(city, vehicle, parsedRideTime);
        return ResponseEntity.ok(estimate);
    }
}
