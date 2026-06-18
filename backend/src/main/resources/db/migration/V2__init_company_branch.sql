CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(255) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE TABLE company_settings (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL UNIQUE REFERENCES companies (id),
    timezone VARCHAR(255) NOT NULL DEFAULT 'Asia/Tashkent',
    locale VARCHAR(255) NOT NULL DEFAULT 'uz-UZ',
    plan VARCHAR(255) NOT NULL DEFAULT 'MVP',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE TABLE branches (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies (id),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    geofence_radius_meters INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE INDEX idx_branches_company_id ON branches (company_id);

CREATE TABLE branch_schedules (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL REFERENCES branches (id),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    late_threshold_min INTEGER NOT NULL DEFAULT 15,
    is_workday BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_branch_schedules_branch_day UNIQUE (branch_id, day_of_week)
);
