CREATE TABLE payroll_adjustments (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees (id),
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    bonus_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    penalty_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_payroll_adjustments_employee_period UNIQUE (employee_id, year, month)
);

CREATE INDEX idx_payroll_adjustments_period ON payroll_adjustments (year, month);
CREATE INDEX idx_payroll_adjustments_employee_id ON payroll_adjustments (employee_id);
