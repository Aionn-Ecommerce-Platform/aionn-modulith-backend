CREATE TABLE stock_alerts (
    alert_id    VARCHAR(50) PRIMARY KEY,
    user_id     VARCHAR(50) NOT NULL,
    product_id  VARCHAR(50) NOT NULL,
    sku_id      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notified_at TIMESTAMPTZ,
    CONSTRAINT uq_stock_alert_user_sku UNIQUE (user_id, sku_id),
    CONSTRAINT fk_stock_alert_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_alerts_pending_sku ON stock_alerts(sku_id) WHERE notified_at IS NULL;
