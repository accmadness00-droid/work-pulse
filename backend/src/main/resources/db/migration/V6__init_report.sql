CREATE TABLE report_snapshots (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies (id),
    branch_id UUID REFERENCES branches (id),
    snapshot_date DATE NOT NULL,
    total_employees INTEGER NOT NULL DEFAULT 0,
    present_count INTEGER NOT NULL DEFAULT 0,
    late_count INTEGER NOT NULL DEFAULT 0,
    absent_count INTEGER NOT NULL DEFAULT 0,
    leave_count INTEGER NOT NULL DEFAULT 0,
    total_work_minutes INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_report_snapshots_scope_date UNIQUE (company_id, branch_id, snapshot_date)
);

CREATE INDEX idx_report_snapshots_company_date ON report_snapshots (company_id, snapshot_date);
CREATE INDEX idx_report_snapshots_branch_date ON report_snapshots (branch_id, snapshot_date);
