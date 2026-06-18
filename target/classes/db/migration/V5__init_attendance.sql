CREATE TABLE attendance_sessions (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees (id),
    branch_id UUID REFERENCES branches (id),
    date DATE NOT NULL,
    check_in_time TIMESTAMPTZ,
    check_out_time TIMESTAMPTZ,
    check_in_lat NUMERIC(9, 6),
    check_in_lng NUMERIC(9, 6),
    check_out_lat NUMERIC(9, 6),
    check_out_lng NUMERIC(9, 6),
    status VARCHAR(50) NOT NULL DEFAULT 'PRESENT',
    late_minutes INTEGER NOT NULL DEFAULT 0,
    work_minutes INTEGER NOT NULL DEFAULT 0,
    method VARCHAR(50) NOT NULL,
    source_device_id UUID REFERENCES devices (id),
    session_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE INDEX idx_attendance_sessions_employee_date ON attendance_sessions (employee_id, date);
CREATE INDEX idx_attendance_sessions_branch_date ON attendance_sessions (branch_id, date);
CREATE UNIQUE INDEX uk_attendance_sessions_open_session
    ON attendance_sessions (employee_id)
    WHERE check_out_time IS NULL;

CREATE TABLE leaves (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255),
    reviewed_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE INDEX idx_leaves_employee_dates ON leaves (employee_id, start_date, end_date);
