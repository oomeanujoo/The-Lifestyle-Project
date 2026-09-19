package com.thelifestyle.integration.adapter.out.geonames;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// GeoNames' documented error shape: an authentication failure, an exceeded
// rate limit, or a bad parameter all come back as HTTP 200 with a `status`
// object instead of a `geonames` array — never a 4xx/5xx. Without checking
// for this explicitly, every one of those real failures looks identical to
// "searched, found nothing," which is exactly the bug this record exists
// to close. Known values include 10 (auth), 18/19/20 (daily/hourly/weekly
// limit exceeded), 21 (invalid parameter) — this doesn't enumerate them,
// it just surfaces whatever GeoNames actually says.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoNamesStatus(Integer value, String message) {}
