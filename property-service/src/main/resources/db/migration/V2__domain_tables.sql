CREATE TABLE property.city (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(160) NOT NULL,
    country_code char(2) NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    timezone varchar(80),
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT property_city_latitude_check CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT property_city_longitude_check CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX property_city_name_idx ON property.city (lower(name));

CREATE TABLE property.municipality (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL REFERENCES property.city(id),
    name varchar(160) NOT NULL,
    code varchar(40),
    CONSTRAINT property_municipality_city_name_unique UNIQUE (city_id, name)
);

CREATE TABLE property.area (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id uuid NOT NULL REFERENCES property.city(id),
    municipality_id uuid REFERENCES property.municipality(id),
    name varchar(160) NOT NULL,
    CONSTRAINT property_area_city_name_unique UNIQUE (city_id, name)
);

CREATE TABLE property.locality (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id uuid NOT NULL REFERENCES property.area(id),
    name varchar(160) NOT NULL,
    CONSTRAINT property_locality_area_name_unique UNIQUE (area_id, name)
);

CREATE TABLE property.pincode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    locality_id uuid NOT NULL REFERENCES property.locality(id),
    code varchar(16) NOT NULL,
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT property_pincode_locality_code_unique UNIQUE (locality_id, code)
);

CREATE TABLE property.bhk_type (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(40) NOT NULL UNIQUE,
    label varchar(100) NOT NULL
);

CREATE TABLE property.service_addon (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(60) NOT NULL UNIQUE,
    label varchar(120) NOT NULL,
    is_active boolean NOT NULL DEFAULT true
);

CREATE TABLE property.currency (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    iso_code char(3) NOT NULL UNIQUE,
    symbol varchar(12),
    exchange_rate_to_base numeric(18,8),
    rate_captured_at timestamptz,
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT property_currency_rate_check CHECK (exchange_rate_to_base > 0)
);

CREATE TABLE property.property_plan (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title varchar(200) NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT property_plan_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE property.rent_snapshot (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    locality_id uuid NOT NULL REFERENCES property.locality(id),
    bhk_type_id uuid NOT NULL REFERENCES property.bhk_type(id),
    currency_id uuid NOT NULL REFERENCES property.currency(id),
    amount numeric(18,2) NOT NULL,
    source varchar(160) NOT NULL,
    source_url text,
    captured_at timestamptz NOT NULL DEFAULT now(),
    confidence varchar(24) NOT NULL DEFAULT 'UNVERIFIED',
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT property_rent_amount_check CHECK (amount >= 0)
);
CREATE INDEX property_rent_snapshot_lookup_idx ON property.rent_snapshot
    (locality_id, bhk_type_id, captured_at DESC);

CREATE TABLE property.cost_estimate (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    property_plan_id uuid NOT NULL REFERENCES property.property_plan(id) ON DELETE CASCADE,
    locality_id uuid NOT NULL REFERENCES property.locality(id),
    bhk_type_id uuid NOT NULL REFERENCES property.bhk_type(id),
    currency_id uuid NOT NULL REFERENCES property.currency(id),
    total_amount numeric(18,2) NOT NULL,
    computed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT property_cost_total_check CHECK (total_amount >= 0)
);

CREATE TABLE property.cost_estimate_addon (
    cost_estimate_id uuid NOT NULL REFERENCES property.cost_estimate(id) ON DELETE CASCADE,
    service_addon_id uuid NOT NULL REFERENCES property.service_addon(id),
    amount numeric(18,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (cost_estimate_id, service_addon_id),
    CONSTRAINT property_addon_amount_check CHECK (amount >= 0)
);

CREATE TABLE property.ai_recommendation (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    property_plan_id uuid NOT NULL REFERENCES property.property_plan(id) ON DELETE CASCADE,
    provider varchar(40) NOT NULL,
    model varchar(120) NOT NULL,
    prompt_version varchar(40) NOT NULL,
    payload_json jsonb NOT NULL,
    referenced_data jsonb NOT NULL DEFAULT '[]'::jsonb,
    verification_status varchar(20) NOT NULL DEFAULT 'UNVERIFIED',
    acceptance_status varchar(20) NOT NULL DEFAULT 'DRAFT',
    generated_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    CONSTRAINT property_ai_acceptance_check CHECK (acceptance_status IN ('DRAFT', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT property_ai_verification_check CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    CONSTRAINT property_ai_accepted_at_check CHECK (acceptance_status <> 'ACCEPTED' OR accepted_at IS NOT NULL)
);

CREATE TABLE property.audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type varchar(80) NOT NULL,
    entity_id uuid NOT NULL,
    action varchar(40) NOT NULL,
    diff_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    changed_at timestamptz NOT NULL DEFAULT now(),
    changed_by varchar(160)
);
CREATE INDEX property_audit_entity_idx ON property.audit_log (entity_type, entity_id, changed_at DESC);

CREATE FUNCTION property.prevent_rent_snapshot_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rent snapshots are append-only';
END;
$$;
CREATE TRIGGER property_rent_snapshot_append_only
    BEFORE UPDATE OR DELETE ON property.rent_snapshot
    FOR EACH ROW EXECUTE FUNCTION property.prevent_rent_snapshot_change();
