CREATE TABLE IF NOT EXISTS property.schema_marker (
    id integer PRIMARY KEY,
    description varchar(120) NOT NULL
);
INSERT INTO property.schema_marker (id, description) VALUES (1, 'Property schema initialized') ON CONFLICT (id) DO NOTHING;
