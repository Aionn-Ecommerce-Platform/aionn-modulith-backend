ALTER TABLE vouchers
    ADD COLUMN min_order_value NUMERIC(18, 2);

ALTER TABLE vouchers
    ADD CONSTRAINT chk_vouchers_min_order
    CHECK (min_order_value IS NULL OR min_order_value >= 0);
