CREATE TABLE devices (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(255) UNIQUE,
    ip_address VARCHAR(255),
    port INTEGER NOT NULL DEFAULT 80,
    username VARCHAR(255),
    credentials_secret VARCHAR(255),
    api_key_hash VARCHAR(255),
    connection_type VARCHAR(50) NOT NULL DEFAULT 'PUSH',
    last_sync_time TIMESTAMPTZ,
    branch_id UUID NOT NULL REFERENCES branches (id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE INDEX idx_devices_branch_id ON devices (branch_id);

CREATE TABLE device_events (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices (id),
    external_event_id VARCHAR(255),
    event_hash VARCHAR(64) NOT NULL,
    employee_code VARCHAR(255),
    credential_value VARCHAR(255),
    event_time TIMESTAMPTZ NOT NULL,
    direction VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    auth_type VARCHAR(50),
    raw_payload JSONB,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_error VARCHAR(255),
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_device_events_device_hash UNIQUE (device_id, event_hash)
);

CREATE INDEX idx_device_events_processed ON device_events (processed, created_at);
CREATE INDEX idx_device_events_employee_code ON device_events (employee_code);

CREATE TABLE employee_credentials (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees (id),
    credential_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_employee_credentials_type_external UNIQUE (credential_type, external_id)
);

CREATE INDEX idx_employee_credentials_employee_id ON employee_credentials (employee_id);
