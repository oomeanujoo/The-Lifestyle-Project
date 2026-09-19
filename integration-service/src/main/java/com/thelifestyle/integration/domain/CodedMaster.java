package com.thelifestyle.integration.domain;

import java.util.UUID;

// A row from a code/label reference master — lifestyle_master.transport_mode,
// bhk_type, or service_addon all share this exact shape (§16). Generic
// over bespoke: one record instead of three near-identical ones. `id` is
// the stable, authoritative UUID Travel/Property must use for any FK that
// targets one of these masters — never a locally-generated one.
public record CodedMaster(UUID id, String code, String label) {}
