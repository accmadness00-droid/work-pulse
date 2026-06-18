CREATE TABLE employee_schedules (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees (id),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    late_threshold_min INTEGER NOT NULL DEFAULT 15,
    is_workday BOOLEAN NOT NULL DEFAULT TRUE,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_employee_schedules_employee_day UNIQUE (employee_id, day_of_week)
);

CREATE INDEX idx_employee_schedules_employee_id ON employee_schedules (employee_id);
