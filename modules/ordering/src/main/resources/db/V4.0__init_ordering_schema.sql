CREATE TABLE carts (
    cart_id      VARCHAR(50) PRIMARY KEY,
    user_id      VARCHAR(50) NOT NULL,
    voucher_code VARCHAR(50),
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_carts_user ON carts(user_id);

CREATE TABLE cart_items (
    cart_id VARCHAR(50) NOT NULL,
    sku_id  VARCHAR(50) NOT NULL,
    qty     INT         NOT NULL,
    PRIMARY KEY (cart_id, sku_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(cart_id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_qty CHECK (qty > 0)
);

CREATE TABLE orders (
    order_id              VARCHAR(50) PRIMARY KEY,
    parent_order_id       VARCHAR(50),
    user_id               VARCHAR(50) NOT NULL,
    merchant_id           VARCHAR(50) NOT NULL,
    proposal_id           VARCHAR(50),
    payment_method_id     VARCHAR(50),
    payment_id            VARCHAR(50),
    currency              VARCHAR(3)  NOT NULL,
    total_amount          NUMERIC(18,2),
    shipping_fee          NUMERIC(18,2),
    address_id            VARCHAR(50),
    address_full_name     VARCHAR(255),
    address_phone         VARCHAR(30),
    address_line          TEXT,
    address_ward_code     VARCHAR(30),
    address_district_code VARCHAR(30),
    address_province_code VARCHAR(30),
    address_country_code  VARCHAR(5),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code           VARCHAR(50),
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    CONSTRAINT fk_orders_parent FOREIGN KEY (parent_order_id) REFERENCES orders(order_id)
);
CREATE INDEX idx_orders_user           ON orders(user_id);
CREATE INDEX idx_orders_merchant       ON orders(merchant_id);
CREATE INDEX idx_orders_status_created ON orders(status, created_at);
CREATE INDEX idx_orders_parent ON orders(parent_order_id) WHERE parent_order_id IS NOT NULL;

CREATE TABLE order_items (
    order_id       VARCHAR(50) NOT NULL,
    sku_id         VARCHAR(50) NOT NULL,
    qty            INT         NOT NULL,
    unit_price     NUMERIC(18,2) NOT NULL,
    warehouse_id   VARCHAR(50),
    reservation_id VARCHAR(50),
    PRIMARY KEY (order_id, sku_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_qty CHECK (qty > 0)
);

CREATE TABLE order_returns (
    return_id           VARCHAR(50) PRIMARY KEY,
    order_id            VARCHAR(50) NOT NULL,
    user_id             VARCHAR(50) NOT NULL,
    merchant_id         VARCHAR(50) NOT NULL,
    reason              TEXT        NOT NULL,
    evidence_url        TEXT,
    refund_amount       NUMERIC(18,2),
    refund_currency     VARCHAR(3),
    return_warehouse_id VARCHAR(50),
    item_condition      TEXT,
    rejection_reason    TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    version             BIGINT      NOT NULL DEFAULT 0,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at          TIMESTAMPTZ,
    received_at         TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_returns_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
CREATE INDEX idx_order_returns_order    ON order_returns(order_id);
CREATE INDEX idx_order_returns_merchant ON order_returns(merchant_id);
CREATE INDEX idx_order_returns_status   ON order_returns(status);
