CREATE TABLE wishlist_items (
    user_id    VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_wishlist_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE INDEX idx_wishlist_user_added ON wishlist_items(user_id, added_at DESC);
