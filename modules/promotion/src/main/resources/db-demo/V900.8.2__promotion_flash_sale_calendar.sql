-- Flash-sale calendar for the storefront.
--
-- Generates three continuous eight-hour windows per day (00-08, 08-16,
-- 16-24) for the next 120 days, so exactly one slot is RUNNING at any hour.
-- The pre-existing campaign 01M74... is reused as today's first window.
--
-- Registrations seeded by V900.8.0 and V900.8.1 are spread over the first six
-- slots; every later slot receives 23 items rotated through the same product
-- pool. Every intermediate result is a CTE; no temporary tables are created.

WITH days AS (
    SELECT generate_series(
               CURRENT_DATE,
               CURRENT_DATE + 120,
               INTERVAL '1 day'
           )::date AS day
),
windows(slot_no, start_hour, end_hour) AS (
    VALUES (1, 0, 8), (2, 8, 16), (3, 16, 24)
),
slots AS (
    SELECT CASE
               WHEN d.day = CURRENT_DATE AND w.slot_no = 1
                   THEN '01M74HC0WB53JNDNF03G2Z7274'
               ELSE 'FS_' || to_char(d.day, 'YYYYMMDD') || '_' || w.slot_no
           END AS campaign_id,
           (d.day + make_interval(hours => w.start_hour))
               AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_date,
           (d.day + make_interval(hours => w.end_hour))
               AT TIME ZONE 'Asia/Ho_Chi_Minh' AS end_date,
           'Flash Sale ' || to_char(d.day, 'DD/MM/YYYY') || ' (' ||
           lpad(w.start_hour::text, 2, '0') || ':00 - ' ||
           CASE
               WHEN w.end_hour = 24 THEN '24:00'
               ELSE lpad(w.end_hour::text, 2, '0') || ':00'
           END || ')' AS name,
           ROW_NUMBER() OVER (ORDER BY d.day, w.slot_no) - 1 AS slot_index
      FROM days d
      CROSS JOIN windows w
)
INSERT INTO promotion_campaigns
    (campaign_id, name, type, budget, budget_remaining, currency,
     start_date, end_date, created_by, status, version, created_at, updated_at)
SELECT s.campaign_id,
       s.name,
       'FLASH_SALE',
       100000000.00,
       100000000.00,
       'VND',
       s.start_date,
       s.end_date,
       'sys_admin_01',
       CASE
           WHEN s.start_date <= NOW() AND s.end_date > NOW() THEN 'RUNNING'
           WHEN s.start_date > NOW() THEN 'SCHEDULED'
           ELSE 'ENDED'
       END,
       0,
       NOW(),
       NOW()
  FROM slots s
ON CONFLICT (campaign_id) DO UPDATE
   SET name = EXCLUDED.name,
       start_date = EXCLUDED.start_date,
       end_date = EXCLUDED.end_date,
       status = EXCLUDED.status,
       updated_at = NOW();

-- Spread the pre-existing registrations over the first six slots so no slot
-- carries the whole seed pool.
WITH days AS (
    SELECT generate_series(
               CURRENT_DATE,
               CURRENT_DATE + 120,
               INTERVAL '1 day'
           )::date AS day
),
windows(slot_no, start_hour, end_hour) AS (
    VALUES (1, 0, 8), (2, 8, 16), (3, 16, 24)
),
slots AS (
    SELECT CASE
               WHEN d.day = CURRENT_DATE AND w.slot_no = 1
                   THEN '01M74HC0WB53JNDNF03G2Z7274'
               ELSE 'FS_' || to_char(d.day, 'YYYYMMDD') || '_' || w.slot_no
           END AS campaign_id,
           ROW_NUMBER() OVER (ORDER BY d.day, w.slot_no) - 1 AS slot_index
      FROM days d
      CROSS JOIN windows w
),
first_slots AS (
    SELECT campaign_id, slot_index
      FROM slots
     WHERE slot_index < 6
),
seed AS (
    SELECT r.registration_id,
           ROW_NUMBER() OVER (
               ORDER BY r.sold_count DESC, r.registration_id
           ) - 1 AS rn
      FROM flash_sale_registrations r
     WHERE r.registration_id NOT LIKE 'FSR_%'
)
UPDATE flash_sale_registrations AS r
   SET campaign_id = f.campaign_id,
       updated_at = NOW()
  FROM seed s
  JOIN first_slots f ON f.slot_index = s.rn % 6
 WHERE r.registration_id = s.registration_id;

-- Generate 23 items for every slot after the first six, rotated through the
-- product pool that V900.8.0 and V900.8.1 already registered.
WITH days AS (
    SELECT generate_series(
               CURRENT_DATE,
               CURRENT_DATE + 120,
               INTERVAL '1 day'
           )::date AS day
),
windows(slot_no, start_hour, end_hour) AS (
    VALUES (1, 0, 8), (2, 8, 16), (3, 16, 24)
),
slots AS (
    SELECT CASE
               WHEN d.day = CURRENT_DATE AND w.slot_no = 1
                   THEN '01M74HC0WB53JNDNF03G2Z7274'
               ELSE 'FS_' || to_char(d.day, 'YYYYMMDD') || '_' || w.slot_no
           END AS campaign_id,
           ROW_NUMBER() OVER (ORDER BY d.day, w.slot_no) - 1 AS slot_index
      FROM days d
      CROSS JOIN windows w
),
templates AS (
    SELECT DISTINCT ON (r.product_id, r.sku_id)
           r.product_id,
           r.sku_id,
           r.merchant_id,
           r.sale_price,
           r.currency,
           r.sale_stock,
           r.sold_count
      FROM flash_sale_registrations r
     WHERE r.status = 'APPROVED'
     ORDER BY r.product_id, r.sku_id, r.sold_count DESC, r.registration_id
),
numbered AS (
    SELECT t.*,
           ROW_NUMBER() OVER (
               ORDER BY t.sold_count DESC, t.product_id, t.sku_id
           ) AS rn,
           COUNT(*) OVER () AS total
      FROM templates t
),
assignments AS (
    SELECT s.campaign_id,
           n.product_id,
           n.sku_id,
           n.merchant_id,
           n.sale_price,
           n.currency,
           n.sale_stock,
           n.sold_count,
           ROW_NUMBER() OVER (
               PARTITION BY s.campaign_id
               ORDER BY ((n.rn - 1 + s.slot_index * 23) % n.total), n.rn
           ) AS pick
      FROM slots s
      CROSS JOIN numbered n
     WHERE s.slot_index >= 6
)
INSERT INTO flash_sale_registrations
    (registration_id, campaign_id, merchant_id, product_id, sku_id,
     sale_price, currency, sale_stock, sold_count, status, reject_reason,
     submitted_at, decided_at, decided_by, updated_at, version)
SELECT 'FSR_' || a.campaign_id || '_' || lpad(a.pick::text, 3, '0'),
       a.campaign_id,
       a.merchant_id,
       a.product_id,
       a.sku_id,
       a.sale_price,
       a.currency,
       a.sale_stock,
       a.sold_count,
       'APPROVED',
       NULL,
       NOW(),
       NOW(),
       'sys_admin_01',
       NOW(),
       0
  FROM assignments a
 WHERE a.pick <= 23;
