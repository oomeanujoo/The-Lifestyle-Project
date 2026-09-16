package com.thelifestyle.integration.adapter.out.frankfurter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterRates(String base, String date, Map<String, Double> rates) {}
