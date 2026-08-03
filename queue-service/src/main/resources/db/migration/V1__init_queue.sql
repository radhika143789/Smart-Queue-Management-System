-- Services table
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_daily_tokens INTEGER NOT NULL DEFAULT 500,
    avg_service_time_seconds INTEGER NOT NULL DEFAULT 300,
    open_time VARCHAR(10) DEFAULT '09:00',
    close_time VARCHAR(10) DEFAULT '17:00',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Counters table
CREATE TABLE IF NOT EXISTS counters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    staff_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Tokens table
CREATE TABLE IF NOT EXISTS tokens (
    id BIGSERIAL PRIMARY KEY,
    token_number VARCHAR(20) NOT NULL,
    sequence_number INTEGER NOT NULL,
    service_id BIGINT NOT NULL REFERENCES services(id),
    counter_id BIGINT REFERENCES counters(id),
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255),
    user_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    version INTEGER NOT NULL DEFAULT 0,
    booked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    called_at TIMESTAMP WITH TIME ZONE,
    served_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    estimated_wait_seconds INTEGER,
    actual_wait_seconds INTEGER,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- INDEXES (critical for queue performance)
-- Composite index for finding waiting tokens for a service
CREATE INDEX idx_tokens_service_status ON tokens(service_id, status);
-- Partial index: only WAITING tokens (most frequent query)
CREATE INDEX idx_tokens_service_waiting ON tokens(service_id, sequence_number)
    WHERE status = 'WAITING';
-- User's tokens lookup
CREATE INDEX idx_tokens_user_id ON tokens(user_id);
-- Date-based queries (daily reports)
CREATE INDEX idx_tokens_booked_at ON tokens(booked_at DESC);
-- Composite for daily service reports
CREATE INDEX idx_tokens_service_date ON tokens(service_id, booked_at DESC);
-- Counter index
CREATE INDEX idx_counters_service ON counters(service_id);

-- Materialized view: hourly stats per service (refreshed every 15 min by scheduler)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_hourly_queue_stats AS
SELECT
    service_id,
    DATE_TRUNC('hour', booked_at) AS hour,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS tokens_completed,
    COUNT(*) FILTER (WHERE status = 'NO_SHOW') AS tokens_no_show,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') AS tokens_cancelled,
    AVG(actual_wait_seconds) FILTER (WHERE actual_wait_seconds IS NOT NULL) AS avg_wait_seconds,
    MAX(sequence_number) AS max_sequence
FROM tokens
GROUP BY service_id, DATE_TRUNC('hour', booked_at)
WITH DATA;

CREATE UNIQUE INDEX idx_mv_hourly_stats ON mv_hourly_queue_stats(service_id, hour);

-- Materialized view: peak hours per service
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_peak_hours AS
SELECT
    service_id,
    EXTRACT(DOW FROM booked_at) AS day_of_week,
    EXTRACT(HOUR FROM booked_at) AS hour_of_day,
    COUNT(*) AS avg_tokens,
    AVG(actual_wait_seconds) AS avg_wait_seconds
FROM tokens
WHERE booked_at > NOW() - INTERVAL '30 days'
GROUP BY service_id, EXTRACT(DOW FROM booked_at), EXTRACT(HOUR FROM booked_at)
WITH DATA;

CREATE UNIQUE INDEX idx_mv_peak_hours ON mv_peak_hours(service_id, day_of_week, hour_of_day);

-- updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ language 'plpgsql';

CREATE TRIGGER update_services_updated_at BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_counters_updated_at BEFORE UPDATE ON counters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_tokens_updated_at BEFORE UPDATE ON tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed sample data
INSERT INTO services (name, description, location, avg_service_time_seconds, open_time, close_time) VALUES
    ('General Inquiry', 'General inquiries and information', 'Ground Floor, Counter A', 240, '09:00', '17:00'),
    ('Document Submission', 'Submit and verify documents', 'Ground Floor, Counter B', 360, '09:00', '16:00'),
    ('Payment Processing', 'Bill payments and receipts', 'First Floor, Counter C', 180, '10:00', '15:00');

INSERT INTO counters (name, service_id, staff_name) VALUES
    ('Counter A1', 1, 'Staff Member'),
    ('Counter A2', 1, 'Staff Member'),
    ('Counter B1', 2, 'Staff Member'),
    ('Counter C1', 3, 'Staff Member');
