-- Date and city let participants (who have no account) find public events in the search.
-- Both are optional so events created before this migration remain valid.
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_date DATE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS city VARCHAR(120);

-- Supports the public search: filter PUBLIC + ACTIVE, ordered by most recent event date.
CREATE INDEX IF NOT EXISTS idx_events_public_listing
    ON events (visibility, status, event_date DESC NULLS LAST, created_at DESC);
