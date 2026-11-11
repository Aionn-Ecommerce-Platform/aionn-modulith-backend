ALTER TABLE shipping_rates ADD COLUMN express_extra_fee NUMERIC(18,2) NOT NULL DEFAULT 0;
ALTER TABLE shipping_rates ADD COLUMN express_eta_days INT;
