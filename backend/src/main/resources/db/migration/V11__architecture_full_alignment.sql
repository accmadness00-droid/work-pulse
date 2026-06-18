-- Company schema alignment
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS inn VARCHAR(20),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS logo_url TEXT,
    ADD COLUMN IF NOT EXISTS plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES users (id);

UPDATE companies SET inn = tax_id WHERE inn IS NULL AND tax_id IS NOT NULL;

ALTER TABLE companies DROP COLUMN IF EXISTS tax_id;

UPDATE companies c
SET plan = CASE
    WHEN cs.plan IN ('FREE', 'BASIC', 'PRO', 'MVP') THEN
        CASE cs.plan WHEN 'MVP' THEN 'FREE' ELSE cs.plan END
    ELSE 'FREE'
END
FROM company_settings cs
WHERE cs.company_id = c.id
  AND c.plan = 'FREE';

ALTER TABLE company_settings DROP COLUMN IF EXISTS plan;

-- Branch geofence default
UPDATE branches SET geofence_radius_meters = 100 WHERE geofence_radius_meters IS NULL;
ALTER TABLE branches ALTER COLUMN geofence_radius_meters SET DEFAULT 100;

-- Report snapshots architecture schema
ALTER TABLE report_snapshots
    ADD COLUMN IF NOT EXISTS period_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    ADD COLUMN IF NOT EXISTS period_start DATE,
    ADD COLUMN IF NOT EXISTS period_end DATE,
    ADD COLUMN IF NOT EXISTS avg_work_minutes INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS generated_by VARCHAR(50) NOT NULL DEFAULT 'SCHEDULER';

UPDATE report_snapshots
SET period_start = snapshot_date,
    period_end = snapshot_date
WHERE period_start IS NULL AND snapshot_date IS NOT NULL;

ALTER TABLE report_snapshots DROP CONSTRAINT IF EXISTS uk_report_snapshots_scope_date;
ALTER TABLE report_snapshots DROP COLUMN IF EXISTS snapshot_date;

ALTER TABLE report_snapshots
    ADD CONSTRAINT uk_report_snapshots_scope_period
        UNIQUE (company_id, branch_id, period_type, period_start, period_end);

ALTER INDEX IF EXISTS uk_attendance_sessions_open_session RENAME TO uq_open_attendance_session;

-- Device event indexes from architecture
CREATE INDEX IF NOT EXISTS idx_device_events_time ON device_events (event_time);
CREATE INDEX IF NOT EXISTS idx_device_events_unproc
    ON device_events (processed, retry_count)
    WHERE processed = false;

CREATE INDEX IF NOT EXISTS idx_credentials_lookup
    ON employee_credentials (credential_type, external_id)
    WHERE active = true;
