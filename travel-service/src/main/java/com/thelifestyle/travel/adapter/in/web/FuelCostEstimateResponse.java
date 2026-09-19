package com.thelifestyle.travel.adapter.in.web;

public record FuelCostEstimateResponse(double amount, String currency, double fuelPricePerLitre, double vehicleKmPerLitre, String label) {}
