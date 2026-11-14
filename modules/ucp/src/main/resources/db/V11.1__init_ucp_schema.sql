CREATE TABLE ucp_checkout_sessions (
    id             VARCHAR(80)  PRIMARY KEY,
    user_id        VARCHAR(120) NOT NULL,
    cart_id        VARCHAR(80),
    status         VARCHAR(30)  NOT NULL,
    currency       VARCHAR(3)   NOT NULL,
    items          JSONB,
    buyer          JSONB,
    context        JSONB,
    order_id       VARCHAR(80),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ,
    price_snapshot JSONB,
    payment        JSONB,
    fulfillment    JSONB,
    voucher_code   VARCHAR(80),
    version        BIGINT       NOT NULL DEFAULT 1
);

CREATE INDEX idx_ucp_checkout_sessions_cart    ON ucp_checkout_sessions(cart_id);
CREATE INDEX idx_ucp_checkout_sessions_order   ON ucp_checkout_sessions(order_id);
CREATE INDEX idx_ucp_checkout_sessions_status  ON ucp_checkout_sessions(status);
CREATE INDEX idx_ucp_checkout_sessions_expires ON ucp_checkout_sessions(expires_at);

CREATE TABLE ucp_identity_links (
    link_id          VARCHAR(80)  PRIMARY KEY,
    platform_id      VARCHAR(120) NOT NULL,
    platform_subject VARCHAR(200) NOT NULL,
    customer_id      VARCHAR(120) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    scopes           JSONB,
    linked_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ,
    metadata         JSONB,
    CONSTRAINT uq_ucp_identity_links_platform_subject UNIQUE (platform_id, platform_subject)
);

CREATE INDEX idx_ucp_identity_links_customer ON ucp_identity_links(customer_id);

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
