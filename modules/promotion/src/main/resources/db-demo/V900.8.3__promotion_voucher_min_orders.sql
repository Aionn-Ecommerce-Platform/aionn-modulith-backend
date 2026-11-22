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
