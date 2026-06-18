ALTER TABLE users
    ADD COLUMN company_id UUID REFERENCES companies (id),
    ADD COLUMN branch_id UUID REFERENCES branches (id);

CREATE INDEX idx_users_company_id ON users (company_id);
CREATE INDEX idx_users_branch_id ON users (branch_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM devices WHERE connection_type = 'PUSH' AND api_key_hash IS NULL
    ) THEN
        ALTER TABLE devices
            ADD CONSTRAINT chk_device_push_key CHECK (
                connection_type <> 'PUSH' OR api_key_hash IS NOT NULL
            );
    END IF;
END $$;

UPDATE leaves SET type = 'ANNUAL' WHERE type = 'VACATION';
