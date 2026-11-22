-- Demo returns for completed orders. Seeded only when the database has no returns yet.
WITH per_merchant AS (
    SELECT DISTINCT ON (o.merchant_id)
           o.order_id,
           o.user_id,
           o.merchant_id,
           o.currency,
           o.completed_at
      FROM orders o
     WHERE o.status = 'COMPLETED'
       AND o.completed_at <= NOW()
     ORDER BY o.merchant_id, o.completed_at DESC, o.order_id
),
picked AS (
    SELECT pm.*,
           ROW_NUMBER() OVER (ORDER BY pm.completed_at DESC, pm.order_id) AS rn
      FROM per_merchant pm
),
seed AS (
    SELECT '01M' || upper(substr(md5(p.order_id || ':' || p.rn::text), 1, 23)) AS return_id,
           p.order_id,
           p.user_id,
           p.merchant_id,
           p.currency,
           p.completed_at,
           i.sku_id,
           i.qty,
           i.unit_price,
           CASE
               WHEN p.rn <= 6 THEN 'REQUESTED'
               WHEN p.rn <= 10 THEN 'APPROVED'
               WHEN p.rn <= 13 THEN 'ITEM_RECEIVED'
               ELSE 'REJECTED'
           END AS status,
           p.rn
      FROM picked p
      JOIN LATERAL (
          SELECT oi.sku_id, oi.qty, oi.unit_price
            FROM order_items oi
           WHERE oi.order_id = p.order_id
           ORDER BY oi.sku_id
           LIMIT 1
      ) i ON TRUE
     WHERE p.rn <= 16
       AND NOT EXISTS (SELECT 1 FROM order_returns)
),
inserted_returns AS (
    INSERT INTO order_returns
        (return_id, order_id, user_id, merchant_id, reason, evidence_url,
         refund_amount, refund_currency, return_warehouse_id, item_condition,
         rejection_reason, status, version, requested_at, decided_at, received_at,
         refund_status, refund_attempts, refund_failure_reason,
         next_refund_attempt_at, updated_at, reason_type)
    SELECT s.return_id,
           s.order_id,
           s.user_id,
           s.merchant_id,
           CASE (s.rn % 4)
               WHEN 0 THEN 'Sản phẩm bị lỗi khi nhận hàng'
               WHEN 1 THEN 'Sản phẩm không đúng mô tả'
               WHEN 2 THEN 'Kích thước không phù hợp'
               ELSE 'Đổi ý, muốn hoàn trả hàng'
           END,
           (
               SELECT oi.image_url
                 FROM order_items oi
                WHERE oi.order_id = s.order_id
                  AND oi.sku_id = s.sku_id
                  AND oi.image_url IS NOT NULL
                  AND btrim(oi.image_url) <> ''
                LIMIT 1
           ),
           s.qty * s.unit_price,
           s.currency,
           NULL,
           'Sản phẩm còn nguyên hộp, chưa qua sử dụng',
           CASE WHEN s.status = 'REJECTED' THEN 'Đã quá thời hạn đổi trả 7 ngày' END,
           s.status,
           0,
           LEAST(NOW(), COALESCE(s.completed_at, NOW()) + INTERVAL '1 day'),
           CASE WHEN s.status <> 'REQUESTED'
                 THEN LEAST(NOW(), COALESCE(s.completed_at, NOW()) + INTERVAL '2 days') END,
           CASE WHEN s.status = 'ITEM_RECEIVED'
                 THEN LEAST(NOW(), COALESCE(s.completed_at, NOW()) + INTERVAL '3 days') END,
           CASE WHEN s.status IN ('APPROVED', 'ITEM_RECEIVED') THEN 'PENDING' ELSE 'NOT_REQUIRED' END,
           0,
           NULL,
           NULL,
           NOW(),
           CASE (s.rn % 4)
               WHEN 0 THEN 'DEFECTIVE'
               WHEN 1 THEN 'NOT_AS_DESCRIBED'
               WHEN 2 THEN 'WRONG_SIZE'
               ELSE 'CHANGED_MIND'
           END
      FROM seed s
    ON CONFLICT (return_id) DO NOTHING
    RETURNING return_id
)
INSERT INTO order_return_items (return_id, sku_id, qty, unit_price, refund_amount)
SELECT s.return_id, s.sku_id, s.qty, s.unit_price, s.qty * s.unit_price
  FROM seed s
  JOIN inserted_returns ir ON ir.return_id = s.return_id
ON CONFLICT DO NOTHING;
