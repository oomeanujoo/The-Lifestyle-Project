package com.thelifestyle.integration.domain;

import java.util.UUID;

// A row read back from lifestyle_master.city — the shape Travel/Property
// consume through the read API (§17/§20). `id` is the stable, authoritative
// UUID Travel/Property must use for any FK that targets a city — never a
// locally-generated one (see LocalMasterMirrorRepository in each service).
// `source` names where this row's data came from (currently always
// "GeoNames", the only city source this service has) and `updatedAt` is
// when it was last refreshed — together the honest freshness/provenance
// answer a consumer (e.g. Travel's route-recommendation endpoint) needs
// before treating a coordinate as current.
public record CityMaster(UUID id, String name, String countryCode, Double latitude, Double longitude, String source, String updatedAt) {}
