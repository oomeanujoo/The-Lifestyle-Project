package com.thelifestyle.property.domain;

// path is the direct navigation target for this match (a city/area/locality
// page), mirroring lifestyle-web's PlaceMatch.to — property search always
// knows exactly where a match leads, unlike travel's filter-only search.
public record Place(String id, String label, String sublabel, PlaceKind kind, String path) {}
