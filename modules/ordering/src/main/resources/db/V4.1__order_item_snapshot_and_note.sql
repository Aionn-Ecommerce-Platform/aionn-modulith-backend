ALTER TABLE orders ADD COLUMN note VARCHAR(1000);

ALTER TABLE order_items ADD COLUMN product_name VARCHAR(500);
ALTER TABLE order_items ADD COLUMN image_url TEXT;
ALTER TABLE order_items ADD COLUMN variant_label VARCHAR(255);
