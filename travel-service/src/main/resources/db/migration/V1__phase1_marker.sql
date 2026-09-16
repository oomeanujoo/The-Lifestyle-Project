CREATE TABLE IF NOT EXISTS travel.schema_marker (
    id integer PRIMARY KEY,
    description varchar(120) NOT NULL
);
INSERT INTO travel.schema_marker (id, description) VALUES (1, 'Travel schema initialized') ON CONFLICT (id) DO NOTHING;
