CREATE TABLE travel.city (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(160) NOT NULL,
    country_code char(2) NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    timezone varchar(80),
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT travel_city_latitude_check CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT travel_city_longitude_check CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX travel_city_name_idx ON travel.city (lower(name));

CREATE TABLE travel.currency (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    iso_code char(3) NOT NULL UNIQUE,
    symbol varchar(12),
    exchange_rate_to_base numeric(18,8),
    rate_captured_at timestamptz,
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT travel_currency_rate_check CHECK (exchange_rate_to_base > 0)
);

CREATE TABLE travel.transport_mode (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(40) NOT NULL UNIQUE,
    label varchar(100) NOT NULL
);

CREATE TABLE travel.visa_requirement (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_country char(2) NOT NULL,
    destination_country char(2) NOT NULL,
    requirement_text text NOT NULL,
    source_url text,
    verified_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE travel.trip_plan (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title varchar(200) NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT travel_trip_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE travel.route_option (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_plan_id uuid NOT NULL REFERENCES travel.trip_plan(id) ON DELETE CASCADE,
    label varchar(160) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX travel_route_option_plan_idx ON travel.route_option (trip_plan_id);

CREATE TABLE travel.trip_leg (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    route_option_id uuid NOT NULL REFERENCES travel.route_option(id) ON DELETE CASCADE,
    from_city_id uuid NOT NULL REFERENCES travel.city(id),
    to_city_id uuid NOT NULL REFERENCES travel.city(id),
    transport_mode_id uuid NOT NULL REFERENCES travel.transport_mode(id),
    leg_order integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT travel_trip_leg_order_check CHECK (leg_order > 0),
    CONSTRAINT travel_trip_leg_order_unique UNIQUE (route_option_id, leg_order)
);
CREATE INDEX travel_trip_leg_from_city_idx ON travel.trip_leg (from_city_id);
CREATE INDEX travel_trip_leg_to_city_idx ON travel.trip_leg (to_city_id);

CREATE TABLE travel.leg_price_quote (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_leg_id uuid NOT NULL REFERENCES travel.trip_leg(id),
    currency_id uuid NOT NULL REFERENCES travel.currency(id),
    amount numeric(18,2) NOT NULL,
    duration_hours numeric(10,2),
    source varchar(160) NOT NULL,
    source_url text,
    captured_at timestamptz NOT NULL DEFAULT now(),
    confidence varchar(24) NOT NULL DEFAULT 'UNVERIFIED',
    provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT travel_price_amount_check CHECK (amount >= 0),
    CONSTRAINT travel_price_duration_check CHECK (duration_hours >= 0)
);
CREATE INDEX travel_leg_price_quote_leg_time_idx ON travel.leg_price_quote (trip_leg_id, captured_at DESC);

CREATE TABLE travel.itinerary_day (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_plan_id uuid NOT NULL REFERENCES travel.trip_plan(id) ON DELETE CASCADE,
    day_number integer NOT NULL,
    title varchar(200) NOT NULL,
    detail text,
    CONSTRAINT travel_itinerary_day_check CHECK (day_number > 0),
    CONSTRAINT travel_itinerary_day_unique UNIQUE (trip_plan_id, day_number)
);

CREATE TABLE travel.document_requirement (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_plan_id uuid NOT NULL REFERENCES travel.trip_plan(id) ON DELETE CASCADE,
    visa_requirement_id uuid REFERENCES travel.visa_requirement(id),
    title varchar(200) NOT NULL,
    verified boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE travel.ai_suggestion (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_plan_id uuid NOT NULL REFERENCES travel.trip_plan(id) ON DELETE CASCADE,
    provider varchar(40) NOT NULL,
    model varchar(120) NOT NULL,
    prompt_version varchar(40) NOT NULL,
    payload_json jsonb NOT NULL,
    referenced_data jsonb NOT NULL DEFAULT '[]'::jsonb,
    verification_status varchar(20) NOT NULL DEFAULT 'UNVERIFIED',
    acceptance_status varchar(20) NOT NULL DEFAULT 'DRAFT',
    generated_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    CONSTRAINT travel_ai_acceptance_check CHECK (acceptance_status IN ('DRAFT', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT travel_ai_verification_check CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    CONSTRAINT travel_ai_accepted_at_check CHECK (acceptance_status <> 'ACCEPTED' OR accepted_at IS NOT NULL)
);

CREATE TABLE travel.audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type varchar(80) NOT NULL,
    entity_id uuid NOT NULL,
    action varchar(40) NOT NULL,
    diff_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    changed_at timestamptz NOT NULL DEFAULT now(),
    changed_by varchar(160)
);
CREATE INDEX travel_audit_entity_idx ON travel.audit_log (entity_type, entity_id, changed_at DESC);

CREATE FUNCTION travel.prevent_price_snapshot_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Price snapshots are append-only';
END;
$$;
CREATE TRIGGER travel_leg_price_quote_append_only
    BEFORE UPDATE OR DELETE ON travel.leg_price_quote
    FOR EACH ROW EXECUTE FUNCTION travel.prevent_price_snapshot_change();
