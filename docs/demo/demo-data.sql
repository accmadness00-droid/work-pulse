-- Optional local/demo data for WorkPulse.
-- Run manually against a local database only. This file is not wired into Flyway
-- and must not be used as a production seed.

INSERT INTO companies (id, name, tax_id, active, created_at)
VALUES ('00000000-0000-0000-0000-000000000101', 'WorkPulse Demo LLC', 'DEMO-INN-001', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO company_settings (id, company_id, timezone, locale, plan, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000102',
    '00000000-0000-0000-0000-000000000101',
    'Asia/Tashkent',
    'uz-UZ',
    'MVP',
    NOW()
)
ON CONFLICT (company_id) DO NOTHING;

INSERT INTO branches (id, company_id, name, address, latitude, longitude, geofence_radius_meters, active, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000101',
    'Main Office',
    'Tashkent, Uzbekistan',
    41.311081,
    69.240562,
    150,
    TRUE,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO branch_schedules (id, branch_id, day_of_week, start_time, end_time, late_threshold_min, is_workday, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000211', '00000000-0000-0000-0000-000000000201', 1, '09:00:00', '18:00:00', 15, TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000212', '00000000-0000-0000-0000-000000000201', 2, '09:00:00', '18:00:00', 15, TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000213', '00000000-0000-0000-0000-000000000201', 3, '09:00:00', '18:00:00', 15, TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000214', '00000000-0000-0000-0000-000000000201', 4, '09:00:00', '18:00:00', 15, TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000215', '00000000-0000-0000-0000-000000000201', 5, '09:00:00', '18:00:00', 15, TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000216', '00000000-0000-0000-0000-000000000201', 6, '09:00:00', '18:00:00', 15, FALSE, NOW()),
    ('00000000-0000-0000-0000-000000000217', '00000000-0000-0000-0000-000000000201', 7, '09:00:00', '18:00:00', 15, FALSE, NOW())
ON CONFLICT ON CONSTRAINT uk_branch_schedules_branch_day DO NOTHING;

INSERT INTO employees (
    id,
    company_id,
    branch_id,
    employee_code,
    first_name,
    last_name,
    position,
    hire_date,
    status,
    hired_date,
    employment_type,
    is_active,
    created_at
)
VALUES
    ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201', 'EMP001', 'Ali', 'Valiyev', 'Engineer', CURRENT_DATE, 'ACTIVE', CURRENT_DATE, 'FULL_TIME', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201', 'EMP002', 'Dilnoza', 'Karimova', 'Accountant', CURRENT_DATE, 'ACTIVE', CURRENT_DATE, 'FULL_TIME', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201', 'EMP003', 'Jasur', 'Ergashev', 'Manager', CURRENT_DATE, 'ACTIVE', CURRENT_DATE, 'FULL_TIME', TRUE, NOW())
ON CONFLICT (employee_code) DO NOTHING;

INSERT INTO devices (
    id,
    name,
    serial_number,
    ip_address,
    port,
    username,
    credentials_secret,
    connection_type,
    branch_id,
    type,
    status,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000000401',
    'HIK-001',
    'HIK-001',
    '192.168.1.50',
    8000,
    'admin',
    'local-demo',
    'PUSH',
    '00000000-0000-0000-0000-000000000201',
    'HIKVISION',
    'ACTIVE',
    NOW()
)
ON CONFLICT (serial_number) DO NOTHING;

INSERT INTO employee_credentials (id, employee_id, credential_type, external_id, active, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000301', 'CARD', 'EMP001-CARD', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-000000000302', 'CARD', 'EMP002-CARD', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-000000000303', 'CARD', 'EMP003-CARD', TRUE, NOW())
ON CONFLICT ON CONSTRAINT uk_employee_credentials_type_external DO NOTHING;
