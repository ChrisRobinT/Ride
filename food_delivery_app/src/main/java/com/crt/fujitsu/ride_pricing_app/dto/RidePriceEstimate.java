package com.crt.fujitsu.ride_pricing_app.dto;

public class RidePriceEstimate {
    private double baseFee;
    private double extraFee;
    private double multiplier;
    private double finalPrice;
    private int riskScore;

    // Constructor
    public RidePriceEstimate(double baseFee, double extraFee, double multiplier, double finalPrice, int riskScore) {
        this.baseFee = baseFee;
        this.extraFee = extraFee;
        this.multiplier = multiplier;
        this.finalPrice = finalPrice;
        this.riskScore = riskScore;
    }

    // Getters and setters
    public double getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(double baseFee) {
        this.baseFee = baseFee;
    }

    public double getExtraFee() {
        return extraFee;
    }

    public void setExtraFee(double extraFee) {
        this.extraFee = extraFee;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }
}
