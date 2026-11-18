-- Flash sale time slots for the storefront.
-- The existing campaign becomes the current window, and two follow-up windows
-- get a round-robin share of its approved items so every slot has a mix of
-- best sellers and slower products.

UPDATE promotion_campaigns
   SET name = 'Flash Sale Hôm Nay',
       start_date = NOW() - INTERVAL '2 hours',
       end_date = NOW() + INTERVAL '4 hours',
       updated_at = NOW()
 WHERE campaign_id = '01M74HC0WB53JNDNF03G2Z7274';

INSERT INTO promotion_campaigns
    (campaign_id, name, type, budget, budget_remaining, currency,
     start_date, end_date, created_by, status, version, created_at, updated_at)
VALUES
    ('SLOT_FLASH_SALE_2', 'Flash Sale Sắp Diễn Ra', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND',
     NOW() + INTERVAL '4 hours', NOW() + INTERVAL '10 hours',
     'sys_admin_01', 'SCHEDULED', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_3', 'Flash Sale Đêm Muộn', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND',
     NOW() + INTERVAL '10 hours', NOW() + INTERVAL '16 hours',
     'sys_admin_01', 'SCHEDULED', 0, NOW(), NOW())
ON CONFLICT (campaign_id) DO NOTHING;

WITH ranked AS (
    SELECT registration_id,
           ROW_NUMBER() OVER (ORDER BY sold_count DESC, registration_id) AS rn
      FROM flash_sale_registrations
     WHERE campaign_id = '01M74HC0WB53JNDNF03G2Z7274'
)
UPDATE flash_sale_registrations AS r
   SET campaign_id = CASE
         WHEN ranked.rn % 3 = 2 THEN 'SLOT_FLASH_SALE_2'
         WHEN ranked.rn % 3 = 0 THEN 'SLOT_FLASH_SALE_3'
         ELSE '01M74HC0WB53JNDNF03G2Z7274'
       END,
       updated_at = NOW()
  FROM ranked
 WHERE r.registration_id = ranked.registration_id;
