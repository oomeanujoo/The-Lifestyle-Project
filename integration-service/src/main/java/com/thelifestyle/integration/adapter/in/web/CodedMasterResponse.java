package com.thelifestyle.integration.adapter.in.web;

// `id` is the stable UUID (as a string) of the lifestyle_master transport_mode
// / bhk_type / service_addon row — the value Travel/Property must persist as
// the FK target, never a locally-generated one.
public record CodedMasterResponse(String id, String code, String label) {}
