-- -----------------------------------------------------------------------------
-- Squashed from V2.0__init_catalog_schema.sql
-- -----------------------------------------------------------------------------
CREATE TABLE merchants (
    merchant_id VARCHAR(50) PRIMARY KEY,
    owner_id    VARCHAR(50) NOT NULL,
    name        VARCHAR(150),
    logo_url    TEXT,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    commission_rate NUMERIC(5,4) NOT NULL DEFAULT 0.05,
    stripe_account_id VARCHAR(100),
    stripe_charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- One merchant per owner (matches MerchantService.register precondition).
CREATE UNIQUE INDEX uq_merchants_owner ON merchants(owner_id);
CREATE INDEX idx_merchants_status ON merchants(status);
CREATE INDEX idx_merchants_stripe_account ON merchants(stripe_account_id) WHERE stripe_account_id IS NOT NULL;
COMMENT ON COLUMN merchants.commission_rate IS 'Platform commission rate as decimal (0.05 = 5%)';

CREATE TABLE categories (
    category_id VARCHAR(50) PRIMARY KEY,
    parent_id   VARCHAR(50),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    icon_url    TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(category_id)
);
CREATE UNIQUE INDEX uq_categories_slug_active
    ON categories(slug)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_parent ON categories(parent_id);
-- Case-insensitive uniqueness within a parent for non-deleted categories
CREATE UNIQUE INDEX uq_categories_parent_name_active
    ON categories(parent_id, LOWER(name))
    WHERE deleted_at IS NULL;

CREATE TABLE brands (
    brand_id    VARCHAR(50) PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    logo_url    TEXT,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Case-insensitive uniqueness, soft-deleted brands keep their row.
CREATE UNIQUE INDEX uq_brands_name_active
    ON brands(LOWER(name))
    WHERE status <> 'DELETED';

CREATE TABLE attribute_templates (
    template_id VARCHAR(50) PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL UNIQUE,
    attributes  JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_attribute_templates_category FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE products (
    product_id     VARCHAR(50) PRIMARY KEY,
    merchant_id    VARCHAR(50) NOT NULL,
    brand_id       VARCHAR(50),
    name           VARCHAR(255) NOT NULL,
    category_ids   JSONB       NOT NULL DEFAULT '[]'::jsonb,
    image_list     JSONB       NOT NULL DEFAULT '[]'::jsonb,
    tags           JSONB       NOT NULL DEFAULT '[]'::jsonb,
    collection_ids JSONB       NOT NULL DEFAULT '[]'::jsonb,
    attributes     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ai_description TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version        BIGINT      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_products_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    CONSTRAINT fk_products_brand    FOREIGN KEY (brand_id)    REFERENCES brands(brand_id)
);
CREATE INDEX idx_products_merchant ON products(merchant_id);
CREATE INDEX idx_products_brand    ON products(brand_id);
CREATE INDEX idx_products_status   ON products(status);
-- jsonb_path_ops: queries must use the @> containment form (not ?, ?? or ?|).
CREATE INDEX idx_products_category_ids_gin ON products USING GIN (category_ids   jsonb_path_ops);
CREATE INDEX idx_products_collections_gin  ON products USING GIN (collection_ids jsonb_path_ops);
CREATE INDEX idx_products_tags_gin         ON products USING GIN (tags           jsonb_path_ops);

CREATE TABLE product_variants (
    sku_id           VARCHAR(50) PRIMARY KEY,
    product_id       VARCHAR(50) NOT NULL,
    attribute_values JSONB       NOT NULL DEFAULT '{}'::jsonb,
    price            NUMERIC(18,2),
    currency         VARCHAR(3),
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);
CREATE INDEX idx_variants_product ON product_variants(product_id);

-- Internationalization tables for catalog (song ngữ)

CREATE TABLE product_translations (
    product_id VARCHAR(50) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    ai_description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, locale),
    CONSTRAINT fk_product_translations_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);
CREATE INDEX idx_product_translations_locale ON product_translations(locale);

CREATE TABLE category_translations (
    category_id VARCHAR(50) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (category_id, locale),
    CONSTRAINT fk_category_translations_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
);
CREATE INDEX idx_category_translations_locale ON category_translations(locale);

CREATE TABLE brand_translations (
    brand_id VARCHAR(50) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (brand_id, locale),
    CONSTRAINT fk_brand_translations_brand FOREIGN KEY (brand_id) REFERENCES brands(brand_id) ON DELETE CASCADE
);
CREATE INDEX idx_brand_translations_locale ON brand_translations(locale);

CREATE TABLE product_reviews (
    review_id                VARCHAR(50) PRIMARY KEY,
    product_id               VARCHAR(50) NOT NULL,
    user_id                  VARCHAR(50) NOT NULL,
    order_id                 VARCHAR(50),
    rating                   SMALLINT NOT NULL,
    title                    VARCHAR(200),
    content                  TEXT,
    image_urls               JSONB NOT NULL DEFAULT '[]'::jsonb,
    status                   VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    merchant_reply           TEXT,
    merchant_replied_at      TIMESTAMPTZ,
    reported_by_merchant_id  VARCHAR(50),
    report_reason            TEXT,
    reported_at              TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE UNIQUE INDEX uq_reviews_user_product ON product_reviews(user_id, product_id);
CREATE INDEX idx_reviews_product ON product_reviews(product_id, created_at DESC);
CREATE INDEX idx_reviews_user ON product_reviews(user_id);
CREATE INDEX idx_reviews_status ON product_reviews(status);
CREATE INDEX idx_reviews_reported ON product_reviews(status) WHERE status = 'REPORTED';

-- -----------------------------------------------------------------------------
-- Squashed from V2.2__seed_review_data.sql
-- -----------------------------------------------------------------------------
-- Seed Product Reviews
-- Only adding reviews for products that users have completed orders for
-- Using realistic Vietnamese reviews with proper ratings and feedback

-- User 01KV05RTFCABHZSQJR4BTNKB4K reviews for their purchased products
-- -----------------------------------------------------------------------------
-- Squashed from V2.4__add_original_price_to_product_variants.sql
-- -----------------------------------------------------------------------------
-- ALTER BẢNG PRODUCT_VARIANTS ĐỂ THÊM CỘT ORIGINAL_PRICE CHO DISCOUNT/SALE
ALTER TABLE product_variants ADD COLUMN original_price NUMERIC(18, 2);

-- -----------------------------------------------------------------------------
-- Squashed from V2.6__product_sold_counters.sql
-- -----------------------------------------------------------------------------
-- =====================================================================
-- PRODUCT SOLD COUNTERS — total units sold per product. Updated by an
-- ordering-side listener (TODO) on completed orders; seed data here so
-- the storefront has realistic "Đã bán X" badges from day one.
-- =====================================================================

CREATE TABLE product_sold_counters (
    product_id  VARCHAR(50) PRIMARY KEY,
    sold_count  BIGINT      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sold_count_nonneg CHECK (sold_count >= 0)
);

-- -----------------------------------------------------------------------------
-- Squashed from V2.7__add_merchant_province.sql
-- -----------------------------------------------------------------------------
-- Add storefront province to merchants. Codes are the canonical zero-padded
-- VN GSO codes seeded by identity (V1.1__seed_full_vn_geography.sql); the
-- denormalized provinceName is a snapshot for read-side display so the search
-- index doesn't have to join across modules.
ALTER TABLE merchants ADD COLUMN province_code VARCHAR(10);
ALTER TABLE merchants ADD COLUMN province_name VARCHAR(100);
CREATE INDEX idx_merchants_province ON merchants(province_code);




-- -----------------------------------------------------------------------------
-- Product-detail description enrichment for all seeded products.
-- -----------------------------------------------------------------------------


-- -----------------------------------------------------------------------------
-- Squashed from V2.1__create_catalog_settings.sql
-- -----------------------------------------------------------------------------
CREATE TABLE catalog_settings (
    key   VARCHAR(100) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO catalog_settings (key, value) VALUES ('default_commission_rate', '0.0500');

-- -----------------------------------------------------------------------------
-- Squashed from V2.3__user_browsing_history.sql
-- -----------------------------------------------------------------------------
-- Per-user recent category/brand preferences, used for personalized feeds.
CREATE TABLE user_browsing_history (
    user_id      VARCHAR(50) PRIMARY KEY,
    category_ids JSONB       NOT NULL DEFAULT '[]'::jsonb,
    brand_ids    JSONB       NOT NULL DEFAULT '[]'::jsonb,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
