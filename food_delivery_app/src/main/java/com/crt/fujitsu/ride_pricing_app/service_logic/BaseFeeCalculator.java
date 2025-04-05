package com.crt.fujitsu.ride_pricing_app.service_logic;

import java.util.HashMap;
import java.util.Map;

public class BaseFeeCalculator {
    public static double getBaseFee(String city, String vehicle) {
        Map<String, Double> baseFees = new HashMap<>();
        baseFees.put("Tallinn,Scooter", 3.5);
        baseFees.put("Tallinn,Bike", 3.0);
        baseFees.put("Tallinn,Car", 4.0);
        baseFees.put("Tartu,Scooter", 3.0);
        baseFees.put("Tartu,Bike", 2.5);
        baseFees.put("Tartu,Car", 3.5);
        baseFees.put("Pärnu,Scooter", 2.5);
        baseFees.put("Pärnu,Bike", 2.0);
        baseFees.put("Pärnu,Car", 3.0);

        String key = city + "," + vehicle;
        return baseFees.getOrDefault(key, 0.0);
    }

}
