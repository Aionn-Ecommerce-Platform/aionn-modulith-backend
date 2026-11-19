-- Flash sale time slots for the storefront.
-- Three short windows per day across two days, with breaks between windows and
-- no window crossing midnight. Items are redistributed round-robin so every
-- slot mixes best sellers with slower products.

INSERT INTO promotion_campaigns
    (campaign_id, name, type, budget, budget_remaining, currency,
     start_date, end_date, created_by, status, version, created_at, updated_at)
VALUES
    ('01M74HC0WB53JNDNF03G2Z7274', 'Flash Sale 08:00 - 11:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'RUNNING', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_2', 'Flash Sale 14:00 - 17:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'RUNNING', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_3', 'Flash Sale 19:00 - 22:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'RUNNING', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_4', 'Flash Sale 08:00 - 11:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'SCHEDULED', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_5', 'Flash Sale 14:00 - 17:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'SCHEDULED', 0, NOW(), NOW()),
    ('SLOT_FLASH_SALE_6', 'Flash Sale 19:00 - 22:00', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND', NOW(), NOW() + INTERVAL '1 hour', 'sys_admin_01', 'SCHEDULED', 0, NOW(), NOW())
ON CONFLICT (campaign_id) DO NOTHING;

WITH slots(campaign_id, name, day_offset, start_hour, end_hour, status) AS (VALUES
    ('01M74HC0WB53JNDNF03G2Z7274', 'Flash Sale 08:00 - 11:00', 0, 8, 11, 'RUNNING'),
    ('SLOT_FLASH_SALE_2', 'Flash Sale 14:00 - 17:00', 0, 14, 17, 'RUNNING'),
    ('SLOT_FLASH_SALE_3', 'Flash Sale 19:00 - 22:00', 0, 19, 22, 'RUNNING'),
    ('SLOT_FLASH_SALE_4', 'Flash Sale 08:00 - 11:00', 1, 8, 11, 'SCHEDULED'),
    ('SLOT_FLASH_SALE_5', 'Flash Sale 14:00 - 17:00', 1, 14, 17, 'SCHEDULED'),
    ('SLOT_FLASH_SALE_6', 'Flash Sale 19:00 - 22:00', 1, 19, 22, 'SCHEDULED')
),
day AS (
    SELECT date_trunc('day', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_of_day
)
UPDATE promotion_campaigns AS c
   SET name = s.name,
       start_date = day.start_of_day
                    + make_interval(days => s.day_offset, hours => s.start_hour),
       end_date = day.start_of_day
                  + make_interval(days => s.day_offset, hours => s.end_hour),
       status = s.status,
       updated_at = NOW()
  FROM slots s, day
 WHERE c.campaign_id = s.campaign_id;

WITH ranked AS (
    SELECT registration_id,
           ROW_NUMBER() OVER (ORDER BY sold_count DESC, registration_id) AS rn
      FROM flash_sale_registrations
)
UPDATE flash_sale_registrations AS r
   SET campaign_id = (ARRAY[
         '01M74HC0WB53JNDNF03G2Z7274',
         'SLOT_FLASH_SALE_2',
         'SLOT_FLASH_SALE_3',
         'SLOT_FLASH_SALE_4',
         'SLOT_FLASH_SALE_5',
         'SLOT_FLASH_SALE_6'
       ])[1 + (ranked.rn % 6)],
       updated_at = NOW()
  FROM ranked
 WHERE r.registration_id = ranked.registration_id;
