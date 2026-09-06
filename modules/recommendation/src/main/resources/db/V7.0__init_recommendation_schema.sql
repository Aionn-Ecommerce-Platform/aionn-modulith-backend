-- Recommendation module schema.
--
-- Cross-module identifiers (user_id, product_id) are stored as plain values without foreign keys:
-- the owning aggregates live in other modules, so referential integrity is enforced at the
-- integration boundary instead. Dangling identifiers are filtered when results are hydrated
-- through the catalog query port.
--
-- Recommendations operate on products, not SKUs. sku_id is a technical identifier used by
-- inventory, pricing and ordering integrations, and a product may span many SKUs.

-- Append-only behavioural log. Rows are inserted by the interaction listener and removed only by
-- the prune scheduler or on account deletion; they are never updated.
CREATE TABLE recommendation_interactions (
    interaction_id   VARCHAR(50)  PRIMARY KEY,
    user_id          VARCHAR(50)  NOT NULL,
    product_id       VARCHAR(50)  NOT NULL,
    interaction_type VARCHAR(20)  NOT NULL,
    weight           NUMERIC(5,2) NOT NULL,
    occurred_at      TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rec_interaction_weight CHECK (weight > 0)
);

CREATE INDEX idx_rec_interactions_user_time
    ON recommendation_interactions (user_id, occurred_at DESC);

CREATE INDEX idx_rec_interactions_product_time
    ON recommendation_interactions (product_id, occurred_at DESC);

-- Supports the co-occurrence self-join, which filters on strong interaction types first.
CREATE INDEX idx_rec_interactions_type_user
    ON recommendation_interactions (interaction_type, user_id);

-- Supports the incremental profile refresh sweep and the age-based prune.
CREATE INDEX idx_rec_interactions_occurred_at
    ON recommendation_interactions (occurred_at);

-- Aggregated affinity per user, rewritten by the profile refresh scheduler. Scores are normalised
-- to [0,1] so content scoring can combine them with other signals without rescaling.
CREATE TABLE recommendation_user_profiles (
    user_id             VARCHAR(50) PRIMARY KEY,
    category_affinities JSONB       NOT NULL DEFAULT '{}'::jsonb,
    brand_affinities    JSONB       NOT NULL DEFAULT '{}'::jsonb,
    price_band_min      NUMERIC(19,2),
    price_band_max      NUMERIC(19,2),
    interaction_count   INTEGER     NOT NULL DEFAULT 0,
    last_interaction_at TIMESTAMPTZ,
    refreshed_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rec_profile_price_band CHECK (
        price_band_min IS NULL OR price_band_max IS NULL OR price_band_min <= price_band_max)
);

-- Item-to-item similarity, precomputed offline. Each unordered pair is stored twice so a lookup by
-- product_id needs no OR predicate and can use a single index.
CREATE TABLE recommendation_item_similarity (
    product_id         VARCHAR(50)  NOT NULL,
    similar_product_id VARCHAR(50)  NOT NULL,
    score              NUMERIC(6,5) NOT NULL,
    co_occurrence      INTEGER      NOT NULL,
    computed_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, similar_product_id),
    CONSTRAINT chk_rec_similarity_distinct CHECK (product_id <> similar_product_id),
    CONSTRAINT chk_rec_similarity_score CHECK (score >= 0 AND score <= 1),
    CONSTRAINT chk_rec_similarity_co_occurrence CHECK (co_occurrence > 0)
);

CREATE INDEX idx_rec_similarity_lookup
    ON recommendation_item_similarity (product_id, score DESC);

-- Time-decayed popularity, distinct from catalog's lifetime product_sold_counters: this measures
-- recent momentum (TRENDING), not total sales (BEST_SELLER).
CREATE TABLE recommendation_popularity (
    product_id       VARCHAR(50)   PRIMARY KEY,
    popularity_score NUMERIC(12,5) NOT NULL,
    view_count       BIGINT        NOT NULL DEFAULT 0,
    purchase_count   BIGINT        NOT NULL DEFAULT 0,
    computed_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rec_popularity_score CHECK (popularity_score >= 0)
);

CREATE INDEX idx_rec_popularity_score
    ON recommendation_popularity (popularity_score DESC);
