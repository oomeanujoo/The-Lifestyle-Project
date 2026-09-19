-- Refresh logs stay in each service's own schema, per
-- scripts/lifestyle-master-schema.sql's own comment — lifestyle_master
-- holds reference data only. This table logs every attempt by
-- integration-service to refresh lifestyle_master.city/currency, success
-- or failure, so nothing about a refresh run silently vanishes (§17.3).
CREATE TABLE integration.master_refresh_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    master_name varchar(80) NOT NULL,
    status varchar(20) NOT NULL,
    records_upserted integer NOT NULL DEFAULT 0,
    error_message text,
    started_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT integration_master_refresh_status_check CHECK (status IN ('SUCCESS', 'PARTIAL', 'FAILED'))
);
CREATE INDEX integration_master_refresh_log_name_idx ON integration.master_refresh_log (master_name, completed_at DESC);
