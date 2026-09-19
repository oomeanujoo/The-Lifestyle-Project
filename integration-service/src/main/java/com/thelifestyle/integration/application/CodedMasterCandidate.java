package com.thelifestyle.integration.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The shape MasterAcquisitionUseCase expects back from an AI provider for
// any CodedMasterType (transport_mode/bhk_type/service_addon all share this
// exact shape, matching PromptDefinition's requiredFields for each). Parsed
// as untrusted input — every field is independently validated before use,
// never trusted because it parsed successfully.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodedMasterCandidate(String code, String label, String evidence) {}
