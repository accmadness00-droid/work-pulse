ALTER TABLE employees
    ADD COLUMN middle_name VARCHAR(255),
    ADD COLUMN phone VARCHAR(255),
    ADD COLUMN photo_url VARCHAR(255),
    ADD COLUMN hired_date DATE,
    ADD COLUMN birth_date DATE,
    ADD COLUMN employment_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN salary NUMERIC(19, 2),
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE employees
SET hired_date = hire_date
WHERE hired_date IS NULL;

UPDATE employees
SET is_active = CASE WHEN status = 'ACTIVE' THEN TRUE ELSE FALSE END;

CREATE INDEX idx_employees_active ON employees (is_active);
CREATE INDEX idx_employees_position ON employees (position);
