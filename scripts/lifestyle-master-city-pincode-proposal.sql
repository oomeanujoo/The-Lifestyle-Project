-- PROPOSAL ONLY — NOT EXECUTED ANYWHERE. Written for Codex (or whoever
-- owns the eventual lifestyle_master schema cutover, §21 phase L) to
-- review before running against a real database. This file makes no
-- change on its own; nothing in this codebase calls or applies it.
--
-- Why this table is needed: GeoNames' postal-code API (used by
-- integration-service's MasterDataRefreshUseCase.refreshIndianPincodes())
-- returns one flat placeName + admin-region names per postal code — it has
-- no concept matching lifestyle_master.pincode's existing shape, which
-- requires locality_id NOT NULL, and locality itself requires a real
-- area -> municipality -> city chain to already exist (see
-- lifestyle-master-schema.sql). Honestly populating lifestyle_master.pincode
-- from GeoNames data would mean inventing a locality/area/municipality row
-- just to satisfy that FK — explicitly disallowed (do not invent
-- municipality, area, or locality values). Real GeoNames pincode data has
-- instead been staged in integration.city_pincode (integration-service's
-- own schema, no cross-schema FK, plain uuid column) since 2026-09-17 — see
-- TECHNICAL_ARCHITECTURE.md's decision log for that entry.
--
-- This proposal: an ADDITIVE new table, shaped like the staging one, that
-- attaches a postal code directly to a city — no locality/area/municipality
-- dependency at all, because GeoNames genuinely doesn't have that
-- information to offer. This does NOT touch, alter, or replace the
-- existing lifestyle_master.pincode table (still valid for any FUTURE
-- data.gov.in-sourced pincode that *does* carry a verified locality) —
-- the two tables would coexist, one per data source, each honest about
-- what it can support.

BEGIN;

CREATE TABLE IF NOT EXISTS lifestyle_master.city_pincode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL REFERENCES lifestyle_master.city(id),
    pincode varchar(16) NOT NULL,
    place_name varchar(160) NOT NULL,
    admin_name2 varchar(160),
    admin_name3 varchar(160),
    latitude numeric(9,6),
    longitude numeric(9,6),
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT lifestyle_master_city_pincode_unique UNIQUE (city_id, pincode)
);
CREATE INDEX IF NOT EXISTS lifestyle_master_city_pincode_city_idx ON lifestyle_master.city_pincode (city_id);
CREATE INDEX IF NOT EXISTS lifestyle_master_city_pincode_code_idx ON lifestyle_master.city_pincode (pincode);

ALTER TABLE lifestyle_master.city_pincode OWNER TO lifestyle_master_owner;

-- Matches the exact grant pattern in lifestyle-master-schema.sql: only
-- integration_app writes lifestyle_master; travel_app/property_app read.
GRANT SELECT, REFERENCES ON lifestyle_master.city_pincode TO travel_app, property_app;
GRANT SELECT, INSERT, UPDATE ON lifestyle_master.city_pincode TO integration_app;

COMMIT;

-- OPTIONAL, SEPARATE FOLLOW-UP — do not run in the same transaction as the
-- DDL above, and only after integration-service's code is switched to
-- write here instead of integration.city_pincode (a Java change this
-- proposal does not include, since it depends on this table existing
-- first). One-time copy of whatever's already been staged:
--
-- INSERT INTO lifestyle_master.city_pincode
--     (id, city_id, pincode, place_name, admin_name2, admin_name3, latitude, longitude, provider_metadata, updated_at)
-- SELECT id, city_id, pincode, place_name, admin_name2, admin_name3, latitude, longitude,
--     jsonb_build_object('source', source), captured_at
-- FROM integration.city_pincode
-- ON CONFLICT (city_id, pincode) DO NOTHING;
