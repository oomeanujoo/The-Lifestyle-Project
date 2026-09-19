-- Run as the PostgreSQL admin. This schema contains reference masters only.
-- Domain tables and refresh logs remain in their service schemas.
BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lifestyle_master_owner') THEN
        CREATE ROLE lifestyle_master_owner NOLOGIN;
    END IF;
END
$$;

CREATE SCHEMA IF NOT EXISTS lifestyle_master AUTHORIZATION lifestyle_master_owner;

CREATE TABLE IF NOT EXISTS lifestyle_master.city (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(160) NOT NULL,
    country_code char(2) NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    timezone varchar(80),
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT lifestyle_master_city_name_country_unique UNIQUE (name, country_code),
    CONSTRAINT lifestyle_master_city_latitude_check CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT lifestyle_master_city_longitude_check CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX IF NOT EXISTS lifestyle_master_city_name_idx ON lifestyle_master.city (lower(name));

CREATE TABLE IF NOT EXISTS lifestyle_master.currency (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    iso_code char(3) NOT NULL UNIQUE,
    symbol varchar(12),
    exchange_rate_to_base numeric(18,8),
    rate_captured_at timestamptz,
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT lifestyle_master_currency_rate_check CHECK (exchange_rate_to_base > 0)
);

CREATE TABLE IF NOT EXISTS lifestyle_master.transport_mode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(40) NOT NULL UNIQUE,
    label varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS lifestyle_master.visa_requirement (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_country char(2) NOT NULL,
    destination_country char(2) NOT NULL,
    requirement_text text NOT NULL,
    source_url text,
    verified_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS lifestyle_master.municipality (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL REFERENCES lifestyle_master.city(id),
    name varchar(160) NOT NULL,
    code varchar(40),
    CONSTRAINT lifestyle_master_municipality_city_name_unique UNIQUE (city_id, name)
);

CREATE TABLE IF NOT EXISTS lifestyle_master.area (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL REFERENCES lifestyle_master.city(id),
    municipality_id uuid REFERENCES lifestyle_master.municipality(id),
    name varchar(160) NOT NULL,
    CONSTRAINT lifestyle_master_area_city_name_unique UNIQUE (city_id, name)
);

CREATE TABLE IF NOT EXISTS lifestyle_master.locality (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id uuid NOT NULL REFERENCES lifestyle_master.area(id),
    name varchar(160) NOT NULL,
    CONSTRAINT lifestyle_master_locality_area_name_unique UNIQUE (area_id, name)
);

CREATE TABLE IF NOT EXISTS lifestyle_master.pincode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    locality_id uuid NOT NULL REFERENCES lifestyle_master.locality(id),
    code varchar(16) NOT NULL,
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT lifestyle_master_pincode_locality_code_unique UNIQUE (locality_id, code)
);

-- GeoNames supplies a postal place and city, but no verified locality chain.
-- Keep these source-backed rows separate from locality-linked pincode rows.
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

CREATE TABLE IF NOT EXISTS lifestyle_master.bhk_type (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(40) NOT NULL UNIQUE,
    label varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS lifestyle_master.service_addon (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(60) NOT NULL UNIQUE,
    label varchar(120) NOT NULL,
    is_active boolean NOT NULL DEFAULT true
);

ALTER SCHEMA lifestyle_master OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.city OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.currency OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.transport_mode OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.visa_requirement OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.municipality OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.area OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.locality OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.pincode OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.city_pincode OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.bhk_type OWNER TO lifestyle_master_owner;
ALTER TABLE lifestyle_master.service_addon OWNER TO lifestyle_master_owner;

GRANT USAGE ON SCHEMA lifestyle_master TO travel_app, property_app, integration_app;
GRANT SELECT, REFERENCES ON ALL TABLES IN SCHEMA lifestyle_master TO travel_app, property_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA lifestyle_master TO integration_app;

COMMIT;
