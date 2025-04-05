package com.crt.fujitsu.ride_pricing_app.service_logic;

import java.time.LocalDateTime;

public class PriceMultiplierCalculator {
    public static double calculatePriceMultiplier(LocalDateTime rideTime) {
        int hour = rideTime.getHour();
        if ((hour > 6 && hour < 10) || (hour > 15 && hour < 20)) return 1.2;
        else if (hour < 5) return 1.1;
        return 1.0;
    }
}
