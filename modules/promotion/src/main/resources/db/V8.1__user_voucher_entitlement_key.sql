-- Allow several grants of one voucher code per user, deduplicated by a
-- per-delivery entitlement key. Null entitlements keep the old one-voucher-per-
-- user-per-code rule through a functional index.
ALTER TABLE user_vouchers
    ADD COLUMN entitlement_key VARCHAR(100);

DROP INDEX IF EXISTS idx_user_vouchers_user_voucher;

CREATE UNIQUE INDEX idx_user_vouchers_user_voucher
    ON user_vouchers (user_id, voucher_code, COALESCE(entitlement_key, ''));
