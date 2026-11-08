-- Vietnam-standard weight-tiered shipping rates.
--
-- Domestic carriers (GHN/GHTK/Viettel Post) and marketplaces price a parcel as
-- a base fee covering the first `base_weight_gram`, then `step_fee` for every
-- further `step_weight_gram` (rounded up), optionally capped by `max_fee`.
-- `free_shipping_threshold` implements the ubiquitous "freeship từ X" policy.
--
-- Existing rows keep their flat behaviour (no surcharge step); the three
-- baseline domestic zones get the standard 1 kg + 5.000đ/500 g ladder.
ALTER TABLE shipping_rates
    ADD COLUMN IF NOT EXISTS base_weight_gram        INTEGER       NOT NULL DEFAULT 1000,
    ADD COLUMN IF NOT EXISTS step_weight_gram        INTEGER       NOT NULL DEFAULT 500,
    ADD COLUMN IF NOT EXISTS step_fee                NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_fee                 NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS free_shipping_threshold NUMERIC(18,2);

ALTER TABLE shipping_rates
    ADD CONSTRAINT chk_rate_base_weight_pos CHECK (base_weight_gram > 0),
    ADD CONSTRAINT chk_rate_step_weight_pos CHECK (step_weight_gram > 0),
    ADD CONSTRAINT chk_rate_step_fee_nonneg CHECK (step_fee >= 0),
    ADD CONSTRAINT chk_rate_max_fee_nonneg CHECK (max_fee IS NULL OR max_fee >= 0);

UPDATE shipping_rates
   SET step_fee = 5000,
       free_shipping_threshold = 500000
 WHERE zone_code IN ('VN-HN', 'VN-SG', 'VN-DN')
   AND step_fee = 0;
