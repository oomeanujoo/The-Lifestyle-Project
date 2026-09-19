package com.thelifestyle.integration.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;

// `proposedData` is accepted as a raw JSON object (not a String) so the
// caller can send actual structured fields (e.g. {"originCountry":"IN",
// "destinationCountry":"FI","requirementText":"..."}) — this controller
// re-serializes it to text before handing it to the use case, which stores
// it verbatim as jsonb without interpreting any of its fields.
public record MasterProposalRequest(String masterType, JsonNode proposedData, String provenance) {}
