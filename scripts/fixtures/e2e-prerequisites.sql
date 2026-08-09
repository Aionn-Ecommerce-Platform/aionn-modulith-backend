INSERT INTO categories (category_id, name, slug, is_active)
VALUES ('CAT_E2E', 'E2E Products', 'e2e-products', TRUE)
ON CONFLICT (category_id) DO UPDATE
SET is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();
