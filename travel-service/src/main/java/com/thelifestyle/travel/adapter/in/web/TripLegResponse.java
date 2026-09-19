package com.thelifestyle.travel.adapter.in.web;

public record TripLegResponse(String id, String routeOptionId, String fromCityId, String toCityId,
                               String transportModeId, int legOrder, String createdAt) {}
