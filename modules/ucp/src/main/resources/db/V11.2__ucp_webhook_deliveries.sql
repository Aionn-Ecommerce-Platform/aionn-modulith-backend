CREATE TABLE ucp_webhook_deliveries (
    delivery_id     VARCHAR(120) PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    order_id        VARCHAR(80)  NOT NULL,
    webhook_url     TEXT         NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ucp_webhook_status CHECK (status IN ('PENDING', 'DELIVERED', 'DEAD'))
);

CREATE INDEX idx_ucp_webhook_pending
    ON ucp_webhook_deliveries(status, next_attempt_at, created_at);
CREATE INDEX idx_ucp_webhook_order
    ON ucp_webhook_deliveries(order_id, created_at);
