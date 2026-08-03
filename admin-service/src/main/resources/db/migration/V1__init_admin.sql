CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    managed_service_ids TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS managed_services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_daily_tokens INTEGER NOT NULL DEFAULT 500,
    avg_service_time_seconds INTEGER NOT NULL DEFAULT 300,
    open_time VARCHAR(10) DEFAULT '09:00',
    close_time VARCHAR(10) DEFAULT '17:00',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(50),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_occurred ON audit_logs(occurred_at DESC);
CREATE INDEX idx_managed_services_active ON managed_services(is_active);

-- Default system settings
INSERT INTO system_settings (setting_key, setting_value, description, category) VALUES
    ('max_tokens_per_user_per_service', '1', 'Max concurrent tokens a user can hold per service', 'QUEUE'),
    ('default_queue_open_time', '09:00', 'Default opening time for new services', 'QUEUE'),
    ('default_queue_close_time', '17:00', 'Default closing time for new services', 'QUEUE'),
    ('token_expiry_hours', '24', 'Hours after which unused tokens expire', 'QUEUE'),
    ('sms_notifications_enabled', 'true', 'Enable SMS notifications system-wide', 'NOTIFICATION'),
    ('email_notifications_enabled', 'true', 'Enable email notifications system-wide', 'NOTIFICATION'),
    ('max_login_attempts', '5', 'Failed logins before account lock', 'SECURITY'),
    ('account_lock_duration_minutes', '15', 'Duration of account lock after max attempts', 'SECURITY');

-- Seed default SUPER_ADMIN (user_id=1 from auth-service)
INSERT INTO managed_services (name, description, location, avg_service_time_seconds, open_time, close_time, created_by) VALUES
    ('General Inquiry', 'General inquiries and information', 'Ground Floor, Counter A', 240, '09:00', '17:00', 1),
    ('Document Submission', 'Submit and verify documents', 'Ground Floor, Counter B', 360, '09:00', '16:00', 1),
    ('Payment Processing', 'Bill payments and receipts', 'First Floor, Counter C', 180, '10:00', '15:00', 1);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ language 'plpgsql';

CREATE TRIGGER update_admin_users_updated_at BEFORE UPDATE ON admin_users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_managed_services_updated_at BEFORE UPDATE ON managed_services FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_system_settings_updated_at BEFORE UPDATE ON system_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
