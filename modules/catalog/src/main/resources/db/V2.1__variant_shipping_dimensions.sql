-- Production parcel columns for variant shipping quotes.
-- V2.0 already declares them for a fresh database; this migration brings an
-- existing database up to date. All four are required before publish (domain
-- guard), so checkout never prices a guessed parcel.
ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS weight_gram INTEGER,
    ADD COLUMN IF NOT EXISTS length_cm   NUMERIC(8,2),
    ADD COLUMN IF NOT EXISTS width_cm    NUMERIC(8,2),
    ADD COLUMN IF NOT EXISTS height_cm   NUMERIC(8,2);

ALTER TABLE product_variants
    ADD CONSTRAINT chk_variant_weight_pos CHECK (weight_gram IS NULL OR weight_gram > 0),
    ADD CONSTRAINT chk_variant_dims_complete CHECK (
        (length_cm IS NULL AND width_cm IS NULL AND height_cm IS NULL)
        OR (length_cm > 0 AND width_cm > 0 AND height_cm > 0)
    );
