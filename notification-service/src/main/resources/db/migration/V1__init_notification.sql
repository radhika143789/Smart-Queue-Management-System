CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(10) NOT NULL,       -- EMAIL or SMS
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    template_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    reference_id VARCHAR(100),       -- tokenId for tracing
    event_type VARCHAR(50),
    retry_count INTEGER NOT NULL DEFAULT 0,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_reference_id ON notifications(reference_id);
CREATE INDEX idx_notifications_status ON notifications(status) WHERE status IN ('FAILED', 'PENDING');
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_event_type ON notifications(event_type);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ language 'plpgsql';

CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
