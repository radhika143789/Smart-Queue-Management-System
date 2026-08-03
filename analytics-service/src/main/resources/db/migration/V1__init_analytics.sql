CREATE TABLE IF NOT EXISTS token_snapshots (
    id BIGSERIAL PRIMARY KEY,
    token_id BIGINT UNIQUE NOT NULL,
    token_number VARCHAR(20),
    service_id BIGINT NOT NULL,
    service_name VARCHAR(100),
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    booked_at TIMESTAMP WITH TIME ZONE,
    called_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    estimated_wait_seconds INTEGER,
    actual_wait_seconds INTEGER,
    event_type VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_snapshots_service_booked ON token_snapshots(service_id, booked_at DESC);
CREATE INDEX idx_snapshots_service_status ON token_snapshots(service_id, status);
CREATE INDEX idx_snapshots_user ON token_snapshots(user_id);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ language 'plpgsql';

CREATE TRIGGER update_snapshots_updated_at
    BEFORE UPDATE ON token_snapshots
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
