-- Banner media is uploaded by an authorized admin and stored by the configured
-- media provider. Remove legacy fixtures that referenced files owned by the
-- frontend deployment instead of durable uploaded media.
DELETE FROM promotion_banners
WHERE banner_id IN ('BAN_001', 'BAN_002', 'BAN_003', 'BAN_004')
  AND image_url LIKE '/images/banners/%';
