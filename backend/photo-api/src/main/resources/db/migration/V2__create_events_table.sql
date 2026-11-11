CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    photographer_id UUID NOT NULL REFERENCES photographers(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    access_code VARCHAR(100),
    biometric_retention_days INTEGER NOT NULL DEFAULT 90,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_events_photographer_id ON events(photographer_id);
