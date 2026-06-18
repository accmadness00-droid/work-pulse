CREATE TABLE IF NOT EXISTS employee_face_profiles (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    embedding_json TEXT NOT NULL,
    photo_url VARCHAR(1024),
    model_name VARCHAR(100) NOT NULL,
    threshold NUMERIC(8, 5),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
    );

CREATE INDEX IF NOT EXISTS idx_employee_face_profiles_employee_id ON employee_face_profiles(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_face_profiles_active ON employee_face_profiles(active);

ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_in_photo_url VARCHAR(1024);
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_out_photo_url VARCHAR(1024);

ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_in_location_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_out_location_verified BOOLEAN DEFAULT FALSE;

ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_in_face_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_out_face_verified BOOLEAN DEFAULT FALSE;

ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_in_face_distance NUMERIC(8, 5);
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS check_out_face_distance NUMERIC(8, 5);

CREATE TABLE IF NOT EXISTS camera_attendance_logs (
    id UUID PRIMARY KEY,
    attendance_session_id UUID,
    employee_id UUID,
    branch_id UUID,
    action VARCHAR(20) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    accuracy_meters NUMERIC(10, 2),
    location_verified BOOLEAN NOT NULL DEFAULT FALSE,
    face_verified BOOLEAN NOT NULL DEFAULT FALSE,
    face_distance NUMERIC(8, 5),
    photo_url VARCHAR(1024),
    user_agent TEXT,
    ip_address VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_camera_attendance_logs_employee_id ON camera_attendance_logs(employee_id);
CREATE INDEX IF NOT EXISTS idx_camera_attendance_logs_branch_id ON camera_attendance_logs(branch_id);
CREATE INDEX IF NOT EXISTS idx_camera_attendance_logs_created_at ON camera_attendance_logs(created_at);
