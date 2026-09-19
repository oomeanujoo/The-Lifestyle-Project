package com.thelifestyle.integration.adapter.in.web;

// The only way a transport-mode/BHK-type/service-addon value enters this
// app now — no hardcoded Java Map anywhere upstream (see
// MasterDataRefreshUseCase's class comment). A human via Swagger, or a
// future admin UI, POSTs one of these; `code` is the natural key
// (upsert-by-code, §17.1 — posting the same code again updates the label).
public record CodedMasterRequest(String code, String label) {}
