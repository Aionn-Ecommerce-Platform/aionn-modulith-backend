ALTER TABLE product_reviews
    ADD COLUMN sku_id VARCHAR(50);

DROP INDEX IF EXISTS uq_reviews_user_product;

CREATE UNIQUE INDEX uq_reviews_user_order_sku
    ON product_reviews (user_id, order_id, sku_id)
    WHERE order_id IS NOT NULL AND sku_id IS NOT NULL;

CREATE INDEX idx_reviews_order_sku
    ON product_reviews (order_id, sku_id);
