-- -----------------------------------------------------------------------------
-- Squashed from V6.0__init_shipping_schema.sql
-- -----------------------------------------------------------------------------
CREATE TABLE shipments (
    shipment_id            VARCHAR(50) PRIMARY KEY,
    order_id               VARCHAR(50) NOT NULL,
    merchant_id            VARCHAR(50) NOT NULL,
    user_id                VARCHAR(50) NOT NULL,
    tracking_code          VARCHAR(100),
    carrier_order_id       VARCHAR(100),
    label_url              TEXT,
    weight_gram            INT NOT NULL DEFAULT 0,
    length_cm              NUMERIC(8,2),
    width_cm               NUMERIC(8,2),
    height_cm              NUMERIC(8,2),
    to_full_name           VARCHAR(255),
    to_phone               VARCHAR(30),
    to_address_line        TEXT,
    to_ward_code           VARCHAR(30),
    to_district_id         VARCHAR(30),
    to_province_code       VARCHAR(30),
    to_country_code        VARCHAR(5),
    cod_amount             NUMERIC(18,2),
    shipping_fee           NUMERIC(18,2),
    currency               VARCHAR(3),
    current_location       VARCHAR(255),
    shipper_name           VARCHAR(255),
    shipper_phone          VARCHAR(30),
    signature_url          TEXT,
    attempt_count          INT NOT NULL DEFAULT 0,
    last_failure_reason    TEXT,
    issue_type             VARCHAR(50),
    issue_resolution       TEXT,
    expected_delivery_date TIMESTAMPTZ,
    status                 VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    version                BIGINT      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    picked_at              TIMESTAMPTZ,
    delivered_at           TIMESTAMPTZ,
    cancelled_at           TIMESTAMPTZ,
    returned_at            TIMESTAMPTZ
);
CREATE INDEX idx_shipments_order           ON shipments(order_id);
CREATE INDEX idx_shipments_merchant        ON shipments(merchant_id);
CREATE INDEX idx_shipments_user            ON shipments(user_id);
CREATE UNIQUE INDEX idx_shipments_tracking ON shipments(tracking_code) WHERE tracking_code IS NOT NULL;
CREATE INDEX idx_shipments_status          ON shipments(status);
CREATE INDEX idx_shipments_active_tracking ON shipments(status, tracking_code)
    WHERE status NOT IN ('DELIVERED','CANCELLED','RETURNED') AND tracking_code IS NOT NULL;

CREATE TABLE shipping_rates (
    rate_id    VARCHAR(50) PRIMARY KEY,
    zone_code  VARCHAR(50) NOT NULL UNIQUE,
    base_fee   NUMERIC(18,2) NOT NULL,
    currency   VARCHAR(3)    NOT NULL,
    condition  TEXT,
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Squashed from V6.1__seed_shipping_rates.sql
-- -----------------------------------------------------------------------------
-- Seed Shipping Rates

INSERT INTO shipping_rates (rate_id, zone_code, base_fee, currency, condition, version, created_at, updated_at) VALUES
('SR_VN_HN', 'VN-HN', 30000.0, 'VND', 'Hanoi Delivery', 0, NOW(), NOW()),
('SR_VN_SG', 'VN-SG', 35000.0, 'VND', 'HCMC Delivery', 0, NOW(), NOW()),
('SR_VN_DN', 'VN-DN', 40000.0, 'VND', 'Da Nang Delivery', 0, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- Squashed from V6.2__seed_shipment_data.sql
-- -----------------------------------------------------------------------------
-- Intentionally empty: shipment seed data was removed because tracking codes
-- (TRACK_xxxx) don't exist on the GHN sandbox and cause poller errors in dev.
-- Real shipments will be created through the order/checkout flow.
SELECT 1;

