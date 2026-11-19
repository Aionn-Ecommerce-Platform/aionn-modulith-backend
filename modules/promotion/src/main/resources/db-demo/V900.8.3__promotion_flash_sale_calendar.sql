-- Extend the flash-sale calendar up to the demo reporting window (31/01/2027).
--
-- The two-day seed in V900.8.2 is left untouched because vouchers and
-- registrations already reference those campaigns. New slots start the day
-- after the last existing slot, with the same three short windows per day
-- (08-11, 14-17, 19-22) and no window crossing midnight.
--
-- Each new slot receives 23 approved registrations rotated through the
-- existing product pool, so consecutive windows show a different mix of
-- products while every slot has enough items for the storefront.

CREATE TEMP TABLE flash_sale_templates AS
SELECT DISTINCT ON (r.product_id, r.sku_id)
       r.product_id,
       r.sku_id,
       r.merchant_id,
       r.sale_price,
       r.currency,
       r.sale_stock,
       r.sold_count
  FROM flash_sale_registrations r
  JOIN promotion_campaigns c ON c.campaign_id = r.campaign_id
 WHERE c.type = 'FLASH_SALE'
   AND r.status = 'APPROVED'
 ORDER BY r.product_id, r.sku_id, r.sold_count DESC, r.registration_id;

WITH last_slot AS (
    SELECT MAX(end_date) AS last_end
      FROM promotion_campaigns
     WHERE type = 'FLASH_SALE'
),
bounds AS (
    SELECT ((SELECT last_end FROM last_slot) AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + 1 AS first_day
),
days AS (
    SELECT d::date AS day
      FROM bounds,
           generate_series(bounds.first_day,
                           DATE '2027-01-31',
                           INTERVAL '1 day') AS d
),
slot_windows(slot_no, start_hour, end_hour) AS (
    VALUES (1, 8, 11), (2, 14, 17), (3, 19, 22)
),
slots AS (
    SELECT 'FS_' || to_char(d.day, 'YYYYMMDD') || '_' || w.slot_no AS campaign_id,
           'Flash Sale ' || to_char(d.day, 'DD/MM/YYYY') || ' (' ||
             lpad(w.start_hour::text, 2, '0') || ':00 - ' ||
             lpad(w.end_hour::text, 2, '0') || ':00)' AS name,
           (d.day + make_interval(hours => w.start_hour)) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_date,
           (d.day + make_interval(hours => w.end_hour)) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS end_date
      FROM days d
      CROSS JOIN slot_windows w
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
 WHERE NOT EXISTS (
       SELECT 1 FROM promotion_campaigns c WHERE c.campaign_id = s.campaign_id);

WITH template AS (
    SELECT t.product_id,
           t.sku_id,
           t.merchant_id,
           t.sale_price,
           t.currency,
           t.sale_stock,
           t.sold_count,
           ROW_NUMBER() OVER (ORDER BY t.sold_count DESC, t.product_id, t.sku_id) AS rn,
           COUNT(*) OVER () AS total
      FROM flash_sale_templates t
),
new_slots AS (
    SELECT c.campaign_id,
           ROW_NUMBER() OVER (ORDER BY c.start_date) - 1 AS slot_index
      FROM promotion_campaigns c
     WHERE c.type = 'FLASH_SALE'
       AND c.campaign_id LIKE 'FS\_%' ESCAPE '\'
       AND NOT EXISTS (
           SELECT 1 FROM flash_sale_registrations r WHERE r.campaign_id = c.campaign_id)
)
INSERT INTO flash_sale_registrations
    (registration_id, campaign_id, merchant_id, product_id, sku_id,
     sale_price, currency, sale_stock, sold_count, status, reject_reason,
     submitted_at, decided_at, decided_by, updated_at, version)
SELECT 'FSR_' || s.campaign_id || '_' || t.rn,
       s.campaign_id,
       t.merchant_id,
       t.product_id,
       t.sku_id,
       t.sale_price,
       t.currency,
       t.sale_stock,
       t.sold_count,
       'APPROVED',
       NULL,
       NOW(),
       NOW(),
       'sys_admin_01',
       NOW(),
       0
  FROM new_slots s
  CROSS JOIN template t
 WHERE ((t.rn - 1 + s.slot_index) % t.total) < 23;

DROP TABLE flash_sale_templates;
