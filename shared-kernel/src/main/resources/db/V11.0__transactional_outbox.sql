CREATE TABLE outbox_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_kind VARCHAR(20) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    ordering_key VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lease_owner VARCHAR(100),
    lease_until TIMESTAMPTZ,
    last_error TEXT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'DEAD_LETTER'))
);

CREATE INDEX idx_outbox_dispatch ON outbox_events(status, next_attempt_at, occurred_at);
CREATE INDEX idx_outbox_ordering ON outbox_events(ordering_key, occurred_at);

CREATE TABLE event_inbox (
    consumer_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (consumer_id, event_id)
);
