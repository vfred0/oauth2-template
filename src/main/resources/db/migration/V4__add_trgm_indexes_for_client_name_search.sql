CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_client_first_name_trgm
    ON client
    USING gin (lower(first_name) gin_trgm_ops);

CREATE INDEX idx_client_last_name_trgm
    ON client
    USING gin (lower(last_name) gin_trgm_ops);

