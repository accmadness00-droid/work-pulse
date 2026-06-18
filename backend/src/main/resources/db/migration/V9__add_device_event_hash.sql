ALTER TABLE device_events
    ALTER COLUMN event_hash TYPE VARCHAR(128);

ALTER TABLE device_events
    ALTER COLUMN external_event_id DROP NOT NULL;
