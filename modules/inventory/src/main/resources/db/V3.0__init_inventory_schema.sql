CREATE TABLE warehouses (
    warehouse_id   VARCHAR(50) PRIMARY KEY,
    merchant_id    VARCHAR(50) NOT NULL,
    address        TEXT,
    priority_level INT         NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version        BIGINT      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_warehouses_merchant ON warehouses(merchant_id);
CREATE INDEX idx_warehouses_status   ON warehouses(status);

CREATE TABLE inventory_items (
    sku_id           VARCHAR(50) NOT NULL,
    warehouse_id     VARCHAR(50) NOT NULL,
    physical_qty     INT         NOT NULL DEFAULT 0,
    available_qty    INT         NOT NULL DEFAULT 0,
    safety_stock_qty INT         NOT NULL DEFAULT 0,
    is_locked        BOOLEAN     NOT NULL DEFAULT FALSE,
    batch_no         VARCHAR(100),
    expiry_date      DATE,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (sku_id, warehouse_id),
    CONSTRAINT fk_inventory_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    CONSTRAINT chk_inventory_qty_nonneg CHECK (physical_qty >= 0 AND available_qty >= 0 AND available_qty <= physical_qty)
);
-- sku_id alone is covered by the leading column of the composite PK, so
-- only the trailing-column index needs to be materialised here.
CREATE INDEX idx_inventory_warehouse ON inventory_items(warehouse_id);

CREATE TABLE stock_transfers (
    transfer_id        VARCHAR(50) PRIMARY KEY,
    merchant_id        VARCHAR(50) NOT NULL,
    from_warehouse_id  VARCHAR(50) NOT NULL,
    to_warehouse_id    VARCHAR(50) NOT NULL,
    sku_id             VARCHAR(50) NOT NULL,
    qty                INT         NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    initiated_at       TIMESTAMPTZ NOT NULL,
    completed_at       TIMESTAMPTZ,
    cancelled_at       TIMESTAMPTZ,
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_transfers_from FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(warehouse_id),
    CONSTRAINT fk_stock_transfers_to   FOREIGN KEY (to_warehouse_id)   REFERENCES warehouses(warehouse_id),
    CONSTRAINT chk_stock_transfers_diff CHECK (from_warehouse_id <> to_warehouse_id)
);
CREATE INDEX idx_stock_transfers_merchant ON stock_transfers(merchant_id);
CREATE INDEX idx_stock_transfers_status   ON stock_transfers(status);

CREATE TABLE stock_reservations (
    reservation_id VARCHAR(50) PRIMARY KEY,
    sku_id         VARCHAR(50) NOT NULL,
    warehouse_id   VARCHAR(50) NOT NULL,
    order_id       VARCHAR(50),
    qty            INT         NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    reserved_at    TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ,
    decided_at     TIMESTAMPTZ,
    version        BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_stock_reservations_order          ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_status_expires ON stock_reservations(status, expires_at);

CREATE TABLE stock_adjustments (
    adj_id        VARCHAR(50) PRIMARY KEY,
    sku_id        VARCHAR(50) NOT NULL,
    warehouse_id  VARCHAR(50) NOT NULL,
    qty           INT         NOT NULL,
    type          VARCHAR(30) NOT NULL,
    reason        TEXT,
    order_id      VARCHAR(50),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stock_adjustments_sku_warehouse ON stock_adjustments(sku_id, warehouse_id);
CREATE INDEX idx_stock_adjustments_order         ON stock_adjustments(order_id);
