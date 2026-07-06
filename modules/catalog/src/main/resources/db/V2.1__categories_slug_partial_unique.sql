DROP INDEX IF EXISTS uq_categories_slug;

CREATE UNIQUE INDEX uq_categories_slug_active
    ON categories(slug)
    WHERE deleted_at IS NULL;
