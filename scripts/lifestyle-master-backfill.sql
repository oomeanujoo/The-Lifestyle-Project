-- Run after Travel and Property Flyway migrations. Safe to rerun: existing
-- master rows in lifestyle_master are never overwritten or deleted.
BEGIN;

INSERT INTO lifestyle_master.city
    (id, name, country_code, latitude, longitude, timezone, provider_metadata, updated_at)
SELECT id, name, country_code, latitude, longitude, timezone, provider_metadata, updated_at
FROM travel.city
ON CONFLICT (name, country_code) DO NOTHING;

INSERT INTO lifestyle_master.city
    (id, name, country_code, latitude, longitude, timezone, provider_metadata, updated_at)
SELECT id, name, country_code, latitude, longitude, timezone, provider_metadata, updated_at
FROM property.city
ON CONFLICT (name, country_code) DO NOTHING;

INSERT INTO lifestyle_master.currency
    (id, iso_code, symbol, exchange_rate_to_base, rate_captured_at, provider_metadata)
SELECT id, iso_code, symbol, exchange_rate_to_base, rate_captured_at, provider_metadata
FROM travel.currency
ON CONFLICT (iso_code) DO NOTHING;

INSERT INTO lifestyle_master.currency
    (id, iso_code, symbol, exchange_rate_to_base, rate_captured_at, provider_metadata)
SELECT id, iso_code, symbol, exchange_rate_to_base, rate_captured_at, provider_metadata
FROM property.currency
ON CONFLICT (iso_code) DO NOTHING;

INSERT INTO lifestyle_master.transport_mode (id, code, label)
SELECT id, code, label FROM travel.transport_mode
ON CONFLICT (code) DO NOTHING;

INSERT INTO lifestyle_master.visa_requirement
    (id, origin_country, destination_country, requirement_text, source_url, verified_at, updated_at)
SELECT id, origin_country, destination_country, requirement_text, source_url, verified_at, updated_at
FROM travel.visa_requirement
ON CONFLICT (id) DO NOTHING;

INSERT INTO lifestyle_master.bhk_type (id, code, label)
SELECT id, code, label FROM property.bhk_type
ON CONFLICT (code) DO NOTHING;

INSERT INTO lifestyle_master.service_addon (id, code, label, is_active)
SELECT id, code, label, is_active FROM property.service_addon
ON CONFLICT (code) DO NOTHING;

INSERT INTO lifestyle_master.municipality (id, city_id, name, code)
SELECT m.id, target_city.id, m.name, m.code
FROM property.municipality m
JOIN property.city source_city ON source_city.id = m.city_id
JOIN lifestyle_master.city target_city
  ON target_city.name = source_city.name AND target_city.country_code = source_city.country_code
ON CONFLICT (id) DO NOTHING;

INSERT INTO lifestyle_master.area (id, city_id, municipality_id, name)
SELECT a.id, target_city.id, a.municipality_id, a.name
FROM property.area a
JOIN property.city source_city ON source_city.id = a.city_id
JOIN lifestyle_master.city target_city
  ON target_city.name = source_city.name AND target_city.country_code = source_city.country_code
ON CONFLICT (id) DO NOTHING;

INSERT INTO lifestyle_master.locality (id, area_id, name)
SELECT id, area_id, name FROM property.locality
ON CONFLICT (id) DO NOTHING;

INSERT INTO lifestyle_master.pincode (id, locality_id, code, provider_metadata)
SELECT id, locality_id, code, provider_metadata FROM property.pincode
ON CONFLICT (id) DO NOTHING;

COMMIT;
