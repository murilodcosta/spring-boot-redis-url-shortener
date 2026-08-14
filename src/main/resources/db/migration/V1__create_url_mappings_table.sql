CREATE TABLE url_mappings (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10),
    long_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP,
    CONSTRAINT uq_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_code ON url_mappings (short_code);
