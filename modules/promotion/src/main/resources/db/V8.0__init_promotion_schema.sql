CREATE TABLE promotion_campaigns (
    campaign_id            VARCHAR(50) PRIMARY KEY,
    name                   VARCHAR(150) NOT NULL,
    type                   VARCHAR(20) NOT NULL,
    budget                 NUMERIC(18,2) NOT NULL,
    budget_remaining       NUMERIC(18,2) NOT NULL,
    currency               VARCHAR(3)  NOT NULL,
    start_date             TIMESTAMPTZ NOT NULL,
    end_date               TIMESTAMPTZ NOT NULL,
    created_by             VARCHAR(50),
    status                 VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    min_order_value        NUMERIC(18,2),
    applicable_categories  JSONB,
    max_claims_per_user    INT,
    max_uses_per_voucher   INT,
    version                BIGINT      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_dates CHECK (start_date < end_date),
    CONSTRAINT chk_campaign_remaining CHECK (budget_remaining >= 0 AND budget_remaining <= budget)
);
CREATE INDEX idx_campaigns_status_dates ON promotion_campaigns(status, start_date, end_date);

CREATE TABLE vouchers (
    voucher_code    VARCHAR(50) PRIMARY KEY,
    campaign_id     VARCHAR(50) NOT NULL,
    discount_amount NUMERIC(18,2) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    usage_limit     INT NOT NULL,
    used_count      INT NOT NULL DEFAULT 0,
    reserved_count  INT NOT NULL DEFAULT 0,
    valid_from      TIMESTAMPTZ,
    valid_until     TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vouchers_campaign FOREIGN KEY (campaign_id) REFERENCES promotion_campaigns(campaign_id),
    CONSTRAINT chk_vouchers_counts CHECK (used_count >= 0 AND reserved_count >= 0
                                          AND (used_count + reserved_count) <= usage_limit)
);
CREATE INDEX idx_vouchers_campaign ON vouchers(campaign_id);

CREATE TABLE user_vouchers (
    user_voucher_id      VARCHAR(50) PRIMARY KEY,
    voucher_code         VARCHAR(50) NOT NULL,
    user_id              VARCHAR(50) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'CLAIMED',
    reserved_order_id    VARCHAR(50),
    applied_amount       NUMERIC(18,2),
    applied_currency     VARCHAR(3),
    claimed_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reserved_at          TIMESTAMPTZ,
    reserved_expires_at  TIMESTAMPTZ,
    applied_at           TIMESTAMPTZ,
    released_at          TIMESTAMPTZ,
    version              BIGINT      NOT NULL DEFAULT 0,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_vouchers_voucher FOREIGN KEY (voucher_code) REFERENCES vouchers(voucher_code)
);
CREATE INDEX idx_user_vouchers_user                ON user_vouchers(user_id);
CREATE UNIQUE INDEX idx_user_vouchers_user_voucher ON user_vouchers(user_id, voucher_code);
CREATE INDEX idx_user_vouchers_status_expires      ON user_vouchers(status, reserved_expires_at);
CREATE INDEX idx_user_vouchers_order               ON user_vouchers(reserved_order_id);

CREATE TABLE promotion_banners (
    banner_id      VARCHAR(50) PRIMARY KEY,
    title          VARCHAR(150) NOT NULL,
    image_url      VARCHAR(500) NOT NULL,
    link_url       VARCHAR(500) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promotion_banners_active ON promotion_banners(active, display_order);

-- FLASH SALE REGISTRATIONS — merchant signs a SKU into a FLASH_SALE
-- campaign; admin approves so the platform stays curated.

CREATE TABLE flash_sale_registrations (
    registration_id   VARCHAR(50)  PRIMARY KEY,
    campaign_id       VARCHAR(50)  NOT NULL,
    merchant_id       VARCHAR(50)  NOT NULL,
    product_id        VARCHAR(50)  NOT NULL,
    sku_id            VARCHAR(50)  NOT NULL,
    sale_price        NUMERIC(18,2) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    sale_stock        INT          NOT NULL,
    sold_count        INT          NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL,
    reject_reason     TEXT,
    submitted_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_at        TIMESTAMPTZ,
    decided_by        VARCHAR(50),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_flash_sale_campaign FOREIGN KEY (campaign_id)
        REFERENCES promotion_campaigns(campaign_id),
    CONSTRAINT chk_flash_sale_stock CHECK (sale_stock > 0 AND sold_count >= 0
                                            AND sold_count <= sale_stock)
);

CREATE INDEX idx_flash_sale_status_campaign
    ON flash_sale_registrations(status, campaign_id);
CREATE INDEX idx_flash_sale_merchant
    ON flash_sale_registrations(merchant_id, status);
CREATE UNIQUE INDEX uq_flash_sale_campaign_sku
    ON flash_sale_registrations(campaign_id, sku_id);

-- Platform vouchers belong to campaigns; shop vouchers belong to merchants.
ALTER TABLE vouchers
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'PLATFORM',
    ADD COLUMN merchant_id VARCHAR(50);

ALTER TABLE vouchers ALTER COLUMN campaign_id DROP NOT NULL;

ALTER TABLE vouchers
    ADD CONSTRAINT chk_voucher_owner CHECK (
        (scope = 'PLATFORM' AND campaign_id IS NOT NULL AND merchant_id IS NULL)
        OR
        (scope = 'SHOP' AND campaign_id IS NULL AND merchant_id IS NOT NULL)
    );

CREATE INDEX idx_vouchers_merchant ON vouchers(merchant_id);
