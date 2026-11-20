ALTER TABLE order_items
    ADD COLUMN original_unit_price NUMERIC(18, 2);

ALTER TABLE order_returns
    ADD COLUMN reason_type VARCHAR(30) NOT NULL DEFAULT 'OTHER';
