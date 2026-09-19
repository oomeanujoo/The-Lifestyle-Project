package com.thelifestyle.travel.application.port.out;

import java.util.Optional;
import java.util.UUID;

// The ID-mapping mechanism this service needs now that it no longer owns
// master data (§17/§20): travel.city/currency/transport_mode still exist
// (no database changes were made — see the decision log) and trip_leg/
// leg_price_quote still have real FKs into them. Before any domain record
// can reference a city/currency/transport-mode, a LOCAL row must exist to
// satisfy that FK. Each upsert* method takes the CANONICAL lifestyle_master
// id looked up via LifestyleMasterReadPort and, for a brand-new local row,
// inserts it with that SAME id — the local row is a shadow of the shared
// master, not an independently-numbered duplicate (§17/§20, corrected from
// the earlier gen_random_uuid()-per-row design). An already-existing local
// row's id is NEVER altered, to preserve any real FK already pointing at
// it; if that existing id differs from the canonical id passed in, this is
// a genuine mismatch that only a database-level remap (never executed by
// this service) can fix — see the FK/ID mapping report for Codex. This is
// on-demand mirroring, not a refresh: it fires only when a domain record is
// actually being created, never on a schedule.
public interface LocalMasterMirrorRepository {
    UUID upsertCity(UUID canonicalId, String name, String countryCode, Double latitude, Double longitude);
    UUID upsertCurrency(UUID canonicalId, String isoCode, Double exchangeRateToBase);
    UUID upsertTransportMode(UUID canonicalId, String code, String label);

    Optional<UUID> findCityId(String name, String countryCode);
    Optional<UUID> findCurrencyId(String isoCode);
    Optional<UUID> findTransportModeId(String code);
}
