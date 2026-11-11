CREATE TABLE collections (
    collection_id VARCHAR(50) PRIMARY KEY,
    slug          VARCHAR(100) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    image_url     TEXT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collections_active_order ON collections(active, display_order);
