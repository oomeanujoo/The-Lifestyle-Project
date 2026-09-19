-- Staging table for GeoNames Indian postal-code results, in integration's
-- OWN schema — deliberately NOT lifestyle_master.pincode. That table's
-- locality_id column is NOT NULL and requires a real
-- locality -> area -> municipality -> city chain to already exist.
-- GeoNames' postalCodeSearchJSON has no concept matching that Indian
-- real-estate area/locality hierarchy: it returns one flat placeName plus
-- admin-region names per postal code, nothing finer-grained. Honestly
-- populating lifestyle_master.pincode from this data would require
-- inventing a locality/area/municipality row just to satisfy that FK,
-- which the refresh explicitly must not do. This table has no such
-- requirement — it links to lifestyle_master.city only by a plain uuid
-- column (no cross-schema FK; Postgres doesn't support one and ADR-003
-- already rules them out), validated at the application layer.
--
-- See TECHNICAL_ARCHITECTURE.md's decision log for the minimal
-- lifestyle_master schema change (a same-shaped table there, or a nullable
-- locality_id) this data would need before it could honestly move out of
-- staging — not executed here or anywhere, per explicit instruction.
CREATE TABLE integration.city_pincode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL,
    city_name varchar(160) NOT NULL,
    country_code char(2) NOT NULL,
    pincode varchar(16) NOT NULL,
    place_name varchar(160) NOT NULL,
    admin_name2 varchar(160),
    admin_name3 varchar(160),
    latitude numeric(9,6),
    longitude numeric(9,6),
    source varchar(40) NOT NULL DEFAULT 'GeoNames',
    captured_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT integration_city_pincode_unique UNIQUE (city_id, pincode)
);
CREATE INDEX integration_city_pincode_city_idx ON integration.city_pincode (city_id);
CREATE INDEX integration_city_pincode_code_idx ON integration.city_pincode (pincode);
