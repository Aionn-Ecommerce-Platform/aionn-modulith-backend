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
    source_event_id  VARCHAR(100),
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

-- One signal per (event, user, product, type). An order carrying two lines for the same product
-- reaches the listener as one event that produces two rows, and collapses to a single purchase signal
-- here. That matches the rule already applied to quantity: needing five of something is not liking it
-- five times more.
--
-- Leading on source_event_id also serves a lookup by event alone - what a dead-letter replay already
-- wrote - so no second index is needed.
CREATE UNIQUE INDEX uq_rec_interactions_source_event
    ON recommendation_interactions (source_event_id, user_id, product_id, interaction_type)
    WHERE source_event_id IS NOT NULL;

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

-- Erasure marks.
--
-- Deleting an account removes this module's interactions and affinity profile, but two things can put
-- them back afterwards: the profile refresh sweep, which reads its batch of user IDs before processing
-- them and so can refresh an account erased in between, and at-least-once outbox delivery, which can
-- replay a view or purchase event for an account that no longer exists. Neither is stopped by the
-- deletes themselves, because there is nothing left to distinguish "erased" from "never had any
-- behavioural data".
--
-- A mark is the bare user identifier and the time erasure was honoured. It holds no behavioural data
-- and is retained indefinitely: pruning it would reopen the leak it exists to close, and at one row
-- per deleted account it is bounded by account deletions rather than by activity.
CREATE TABLE recommendation_erased_users (
    user_id   VARCHAR(50) PRIMARY KEY,
    erased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
