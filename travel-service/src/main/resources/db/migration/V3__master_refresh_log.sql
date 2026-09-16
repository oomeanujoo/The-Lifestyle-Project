-- Adds what §17's upsert-never-delete refresh mechanism needs that V2 didn't:
-- (1) a natural-key uniqueness constraint on travel.city so a refresh can
--     use INSERT ... ON CONFLICT DO UPDATE instead of blindly inserting
--     duplicates on every run (travel.currency already had one via its
--     iso_code UNIQUE constraint, so nothing to add there);
-- (2) an append-only log of every refresh attempt, per master, so the
--     Settings page can show "last refreshed" honestly from real data.
ALTER TABLE travel.city
    ADD CONSTRAINT travel_city_name_country_unique UNIQUE (name, country_code);

CREATE TABLE travel.master_refresh_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    master_name varchar(80) NOT NULL,
    status varchar(20) NOT NULL,
    records_upserted integer NOT NULL DEFAULT 0,
    error_message text,
    started_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT travel_master_refresh_status_check CHECK (status IN ('SUCCESS', 'PARTIAL', 'FAILED'))
);
CREATE INDEX travel_master_refresh_log_name_idx ON travel.master_refresh_log (master_name, completed_at DESC);
