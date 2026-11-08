-- Demo-only parcel data for the seeded catalog.
--
-- Production catalogues get weight/dimensions from the seller via the variant
-- API (weight is mandatory before publish). The demo fixtures were generated
-- before that column existed, so this backfills realistic values derived from
-- each product's category — a garment is not the same parcel as a laptop.
--
-- The figures below are representative retail parcels (product + packaging):
--   apparel        ~250 g   25 × 20 × 5 cm
--   footwear       ~900 g   33 × 22 × 13 cm
--   skincare/beauty ~350 g  15 × 12 × 8 cm
--   vitamins       ~300 g   12 × 8 × 8 cm
--   phones         ~500 g   18 × 12 × 6 cm
--   laptops        ~2.4 kg  40 × 30 × 8 cm
--   tech accessories ~300 g 18 × 14 × 8 cm
--   smart home / kitchen ~1.2 kg  30 × 25 × 20 cm
--   furniture      ~8 kg    100 × 60 × 40 cm
--   fallback       ~600 g   25 × 20 × 10 cm
--
-- Every product takes the profile of its most specific known category; the
-- first matching rule in category_parcel wins, ordered so leaf categories are
-- checked before their parents.
CREATE TEMP TABLE category_parcel (
    category_id  VARCHAR(50) PRIMARY KEY,
    weight_gram  INTEGER       NOT NULL,
    length_cm    NUMERIC(8, 2) NOT NULL,
    width_cm     NUMERIC(8, 2) NOT NULL,
    height_cm    NUMERIC(8, 2) NOT NULL,
    priority     INTEGER       NOT NULL
);

INSERT INTO category_parcel (category_id, weight_gram, length_cm, width_cm, height_cm, priority) VALUES
    ('CAT_FUR', 8000, 100, 60, 40, 10),   -- furniture
    ('CAT_KAP', 2500,  40, 30, 25, 10),   -- kitchen appliances
    ('CAT_LAP', 2400,  40, 30,  8, 10),   -- laptops
    ('CAT_SMC', 3000,  45, 35, 30, 10),   -- climate control
    ('CAT_PHO',  500,  18, 12,  6, 10),   -- smartphones
    ('CAT_ACC',  300,  18, 14,  8, 10),   -- tech accessories
    ('CAT_SML',  600,  20, 20, 12, 10),   -- smart lighting
    ('CAT_SMS',  700,  20, 20, 12, 10),   -- smart security
    ('CAT_SKN',  350,  15, 12,  8, 10),   -- skincare
    ('CAT_VIT',  300,  12,  8,  8, 10),   -- vitamins
    ('CAT_HBE',  350,  15, 12,  8, 10),   -- health & beauty (parent)
    ('CAT_SHO',  900,  33, 22, 13, 10),   -- footwear
    ('CAT_MCL',  250,  25, 20,  5, 10),   -- mens clothing
    ('CAT_WCL',  250,  25, 20,  5, 10),   -- womens clothing
    ('CAT_FAS',  250,  25, 20,  5, 10),   -- fashion (parent)
    ('CAT_SMH', 1200,  30, 25, 20, 10),   -- smart home (parent)
    ('CAT_ELE',  600,  25, 20, 10, 10),   -- electronics (parent)
    ('CAT_HOK', 1000,  30, 25, 20, 10);   -- home & kitchen (parent)

-- Each variant inherits the parcel of the most specific category its product
-- carries; products whose categories are all unknown fall back to the generic
-- parcel below.
UPDATE product_variants v
   SET weight_gram = COALESCE(pick.weight_gram, 600),
       length_cm   = COALESCE(pick.length_cm,  25),
       width_cm    = COALESCE(pick.width_cm,   20),
       height_cm   = COALESCE(pick.height_cm,  10)
  FROM products p
  LEFT JOIN LATERAL (
        SELECT cp.weight_gram, cp.length_cm, cp.width_cm, cp.height_cm
          FROM category_parcel cp
         WHERE p.category_ids @> to_jsonb(cp.category_id)
         ORDER BY cp.priority ASC, cp.weight_gram DESC
         LIMIT 1
  ) pick ON TRUE
 WHERE v.product_id = p.product_id
   AND v.weight_gram IS NULL;

DROP TABLE category_parcel;
