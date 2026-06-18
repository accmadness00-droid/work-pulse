ALTER TABLE company_settings
    ADD COLUMN payroll_overtime_bonus_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payroll_late_penalty_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payroll_overtime_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.50,
    ADD COLUMN payroll_late_penalty_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.00;
