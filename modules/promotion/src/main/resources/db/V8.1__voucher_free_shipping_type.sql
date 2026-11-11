-- Distinguish goods-discount vouchers from free-shipping vouchers.
-- A FREE_SHIPPING voucher waives the shipping fee, capped by discount_amount
-- (the marketplace "mã freeship 30k" model). Existing rows are goods discounts.
ALTER TABLE vouchers
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'ORDER_DISCOUNT';

ALTER TABLE vouchers
    ADD CONSTRAINT chk_vouchers_type CHECK (type IN ('ORDER_DISCOUNT', 'FREE_SHIPPING'));
