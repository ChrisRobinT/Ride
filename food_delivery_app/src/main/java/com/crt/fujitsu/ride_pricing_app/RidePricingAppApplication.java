package com.crt.fujitsu.ride_pricing_app;

// Name: Chris-Robin Talts

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RidePricingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RidePricingAppApplication.class, args);
	}

}
