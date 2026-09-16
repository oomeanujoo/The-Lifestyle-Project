-- Same additive pattern as travel-service's V3 — see that file's comment.
-- property.currency already has iso_code UNIQUE; only property.city needs a
-- natural-key constraint added for INSERT ... ON CONFLICT to work.
ALTER TABLE property.city
    ADD CONSTRAINT property_city_name_country_unique UNIQUE (name, country_code);

CREATE TABLE property.master_refresh_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    master_name varchar(80) NOT NULL,
    status varchar(20) NOT NULL,
    records_upserted integer NOT NULL DEFAULT 0,
    error_message text,
    started_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT property_master_refresh_status_check CHECK (status IN ('SUCCESS', 'PARTIAL', 'FAILED'))
);
CREATE INDEX property_master_refresh_log_name_idx ON property.master_refresh_log (master_name, completed_at DESC);
