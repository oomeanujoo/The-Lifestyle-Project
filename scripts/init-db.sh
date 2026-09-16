#!/usr/bin/env bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v travel_user="$TRAVEL_DB_USER" -v travel_password="$TRAVEL_DB_PASSWORD" \
  -v property_user="$PROPERTY_DB_USER" -v property_password="$PROPERTY_DB_PASSWORD" \
  -v integration_user="$INTEGRATION_DB_USER" -v integration_password="$INTEGRATION_DB_PASSWORD" \
  -v database_name="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"travel_user" LOGIN PASSWORD :'travel_password';
CREATE ROLE :"property_user" LOGIN PASSWORD :'property_password';
CREATE ROLE :"integration_user" LOGIN PASSWORD :'integration_password';
REVOKE ALL ON DATABASE :"database_name" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"database_name" TO :"travel_user", :"property_user", :"integration_user";
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA travel AUTHORIZATION :"travel_user";
CREATE SCHEMA property AUTHORIZATION :"property_user";
CREATE SCHEMA integration AUTHORIZATION :"integration_user";
SQL
