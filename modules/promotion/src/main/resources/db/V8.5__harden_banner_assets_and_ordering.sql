UPDATE promotion_banners
SET image_public_id = 'legacy/promotion/banners/' || banner_id
WHERE image_public_id IS NULL OR BTRIM(image_public_id) = '';

ALTER TABLE promotion_banners
    ALTER COLUMN image_public_id SET DEFAULT 'legacy/promotion/banners/unmanaged',
    ALTER COLUMN image_public_id SET NOT NULL;

CREATE SEQUENCE promotion_banner_display_order_seq;

SELECT setval(
    'promotion_banner_display_order_seq',
    COALESCE((SELECT MAX(display_order) FROM promotion_banners), 0) + 1,
    FALSE
);
