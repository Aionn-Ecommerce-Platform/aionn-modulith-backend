-- Give every collectible voucher a realistic minimum spend.
--
-- Shop vouchers get a minimum derived from their discount value.
-- Platform campaigns inherit the highest threshold of their vouchers, and the
-- platform vouchers mirror that campaign minimum so summaries expose the same
-- condition the checkout enforces.
UPDATE vouchers
   SET min_order_value = CASE
           WHEN discount_amount >= 300000 THEN 699000
           WHEN discount_amount >= 200000 THEN 499000
           WHEN discount_amount >= 100000 THEN 299000
           WHEN discount_amount >= 75000 THEN 249000
           WHEN discount_amount >= 50000 THEN 199000
           WHEN discount_amount >= 30000 THEN 149000
           WHEN discount_amount >= 20000 THEN 99000
           ELSE 49000
       END
 WHERE scope = 'SHOP'
   AND type = 'ORDER_DISCOUNT'
   AND (min_order_value IS NULL OR min_order_value = 0);

UPDATE promotion_campaigns AS c
   SET min_order_value = t.min_order
  FROM (
      SELECT campaign_id,
             CASE
                 WHEN MAX(discount_amount) >= 300000 THEN 699000
                 WHEN MAX(discount_amount) >= 200000 THEN 499000
                 WHEN MAX(discount_amount) >= 100000 THEN 299000
                 WHEN MAX(discount_amount) >= 75000 THEN 249000
                 WHEN MAX(discount_amount) >= 50000 THEN 199000
                 WHEN MAX(discount_amount) >= 30000 THEN 149000
                 WHEN MAX(discount_amount) >= 20000 THEN 99000
                 ELSE 49000
             END AS min_order
        FROM vouchers
       WHERE scope = 'PLATFORM'
         AND type = 'ORDER_DISCOUNT'
       GROUP BY campaign_id
  ) AS t
 WHERE c.campaign_id = t.campaign_id
   AND (c.min_order_value IS NULL OR c.min_order_value = 0);

UPDATE vouchers AS v
   SET min_order_value = c.min_order_value
  FROM promotion_campaigns AS c
 WHERE v.campaign_id = c.campaign_id
   AND v.scope = 'PLATFORM'
   AND v.min_order_value IS NULL
   AND c.min_order_value IS NOT NULL;


-- Demo chronology: promotions and vouchers must exist before historical orders.
UPDATE promotion_campaigns
SET created_at = TIMESTAMPTZ '2025-02-01 00:00:00+00';

UPDATE vouchers
SET created_at = TIMESTAMPTZ '2025-03-01 00:00:00+00';

-- Moved from app/V900.11.0__demo_admin_operations.sql
WITH upcoming AS (
    SELECT campaign_id
      FROM promotion_campaigns
     WHERE type = 'FLASH_SALE'
       AND campaign_id LIKE 'FS\_%' ESCAPE '\'
       AND start_date > NOW()
     ORDER BY start_date
     LIMIT 5
),
candidates AS (
    SELECT c.campaign_id,
           p.product_id,
           p.merchant_id,
           v.sku_id,
           COALESCE(v.price, 100000) AS price,
           ROW_NUMBER() OVER (ORDER BY c.campaign_id, md5(c.campaign_id || p.product_id)) AS rn
      FROM upcoming c
      CROSS JOIN LATERAL (
          SELECT p.product_id, p.merchant_id, v.sku_id, v.price
            FROM products p
            JOIN product_variants v ON v.product_id = p.product_id
           WHERE p.status = 'PUBLISHED'
             AND NOT EXISTS (
                 SELECT 1 FROM flash_sale_registrations r
                  WHERE r.campaign_id = c.campaign_id AND r.sku_id = v.sku_id)
           ORDER BY md5(c.campaign_id || p.product_id)
           LIMIT 3
      ) p
      JOIN product_variants v ON v.sku_id = p.sku_id
)
INSERT INTO flash_sale_registrations
    (registration_id, campaign_id, merchant_id, product_id, sku_id,
     sale_price, currency, sale_stock, sold_count, status, reject_reason,
     submitted_at, decided_at, decided_by, updated_at, version)
SELECT 'FSR_PENDING_' || lpad(c.rn::text, 4, '0'),
       c.campaign_id,
       c.merchant_id,
       c.product_id,
       c.sku_id,
       ROUND(c.price * 0.9),
       'VND',
       40,
       0,
       'PENDING',
       NULL,
       NOW() - make_interval(hours => c.rn::int),
       NULL,
       NULL,
       NOW() - make_interval(hours => c.rn::int),
       0
  FROM candidates c
ON CONFLICT DO NOTHING;

-- Moved from app/V900.11.0__demo_admin_operations.sql
WITH merchants_ranked AS (
    SELECT merchant_id,
           ROW_NUMBER() OVER (ORDER BY name, merchant_id) AS rn
      FROM merchants
),
templates(discount_amount, type, prefix) AS (
    VALUES (20000.00, 'ORDER_DISCOUNT', 'SHOP10'),
           (50000.00, 'ORDER_DISCOUNT', 'SHOP25')
)
INSERT INTO vouchers
    (voucher_code, campaign_id, discount_amount, currency, usage_limit,
     used_count, reserved_count, valid_from, valid_until, scope, type,
     min_order_value, merchant_id, version, created_at, updated_at)
SELECT t.prefix || '-' || lpad(m.rn::text, 3, '0'),
       NULL,
       t.discount_amount,
       'VND',
       200,
       20,
       0,
       NOW() - INTERVAL '7 days',
       NOW() + INTERVAL '120 days',
       'SHOP',
       t.type,
       CASE
           WHEN t.discount_amount >= 300000 THEN 699000
           WHEN t.discount_amount >= 200000 THEN 499000
           WHEN t.discount_amount >= 100000 THEN 299000
           WHEN t.discount_amount >= 75000 THEN 249000
           WHEN t.discount_amount >= 50000 THEN 199000
           WHEN t.discount_amount >= 30000 THEN 149000
           WHEN t.discount_amount >= 20000 THEN 99000
           ELSE 49000
       END,
       m.merchant_id,
       0,
       NOW() - INTERVAL '7 days',
       NOW()
  FROM merchants_ranked m
  CROSS JOIN templates t
ON CONFLICT (voucher_code) DO NOTHING;

-- Moved from app/V900.11.3__demo_remaining_tables.sql
WITH picked_orders AS (
    SELECT order_id,
           ROW_NUMBER() OVER (ORDER BY order_id) AS rn
      FROM orders
     WHERE status IN ('APPROVED', 'PREPARING', 'SHIPPED', 'COMPLETED')
     ORDER BY order_id
     LIMIT 10
),
picked_regs AS (
    SELECT registration_id,
           ROW_NUMBER() OVER (ORDER BY registration_id) AS rn
      FROM flash_sale_registrations
     WHERE status = 'APPROVED'
     ORDER BY registration_id
     LIMIT 10
)
INSERT INTO flash_sale_allocations
    (order_id, registration_id, quantity, created_at, released_at)
SELECT o.order_id,
       r.registration_id,
       1,
       NOW() - make_interval(days => (o.rn % 10)::int),
       CASE WHEN o.rn > 7 THEN NOW() - make_interval(days => (o.rn % 3)::int) END
  FROM picked_orders o
  JOIN picked_regs r ON r.rn = o.rn
ON CONFLICT (order_id, registration_id) DO NOTHING;
