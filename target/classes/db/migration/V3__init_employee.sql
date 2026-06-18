CREATE TABLE employees (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies (id),
    branch_id UUID NOT NULL REFERENCES branches (id),
    user_id UUID UNIQUE REFERENCES users (id),
    employee_code VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    hire_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users (id)
);

CREATE INDEX idx_employees_company_id ON employees (company_id);
CREATE INDEX idx_employees_branch_id ON employees (branch_id);
