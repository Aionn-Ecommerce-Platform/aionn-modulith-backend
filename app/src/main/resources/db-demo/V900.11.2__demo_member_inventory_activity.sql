WITH picked_users AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS user_rank
      FROM users
     ORDER BY user_id
     LIMIT 20
),
slots AS (
    SELECT generate_series(0, 4) AS slot
)
INSERT INTO security_audits
    (audit_id, user_id, event_type, description, ip_address, device_id, timestamp)
SELECT '01M9AUD' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_rank, s.slot)::text, 19, '0'),
       u.user_id,
       CASE (u.user_rank + s.slot) % 8
           WHEN 0 THEN 'PASSWORD_CHANGED'
           WHEN 1 THEN 'PASSWORD_RESET_REQUESTED'
           WHEN 2 THEN 'PASSWORD_RESET_COMPLETED'
           WHEN 3 THEN 'MFA_SETUP_INITIATED'
           WHEN 4 THEN 'MFA_ENABLED'
           WHEN 5 THEN 'MFA_DISABLED'
           WHEN 6 THEN 'MFA_BACKUP_CODES_REGENERATED'
           ELSE 'AGENT_SUSPENDED'
       END,
       CASE (u.user_rank + s.slot) % 8
           WHEN 0 THEN 'Đổi mật khẩu tài khoản'
           WHEN 1 THEN 'Yêu cầu đặt lại mật khẩu'
           WHEN 2 THEN 'Hoàn tất đặt lại mật khẩu'
           WHEN 3 THEN 'Bắt đầu thiết lập xác thực 2 lớp'
           WHEN 4 THEN 'Bật xác thực 2 lớp'
           WHEN 5 THEN 'Tắt xác thực 2 lớp'
           WHEN 6 THEN 'Tạo lại mã dự phòng'
           ELSE 'Tạm khóa một AI agent'
       END,
       '203.113.1.' || ((u.user_rank * 7 + s.slot * 13) % 250),
       'device-demo-' || u.user_rank || '-' || s.slot,
       NOW() - make_interval(days => ((u.user_rank * 3 + s.slot) % 90)::int,
                             hours => (s.slot * 2)::int)
  FROM picked_users u
  CROSS JOIN slots s
ON CONFLICT (audit_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 20
),
cards(card_no, provider, last_4) AS (
    VALUES (1, 'STRIPE', '4242'), (2, 'VISA', '1881')
)
INSERT INTO payment_methods
    (method_id, user_id, provider, last_4_digits, gateway_token, status,
     version, created_at, updated_at, verified_at)
SELECT 'PM_DEMO_' || lpad(((b.rn - 1) * 2 + c.card_no)::text, 5, '0'),
       b.user_id,
       c.provider,
       c.last_4,
       'pm_demo_' || b.rn || '_' || c.card_no,
       'LINKED',
       0,
       TIMESTAMPTZ '2025-02-15 00:00:00+00' + make_interval(days => b.rn::int),
       TIMESTAMPTZ '2025-02-15 00:00:00+00' + make_interval(days => b.rn::int),
       TIMESTAMPTZ '2025-02-15 00:00:00+00' + make_interval(days => b.rn::int)
  FROM buyers b
  CROSS JOIN cards c
ON CONFLICT (method_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 20
)
INSERT INTO wishlist_items (user_id, product_id, added_at)
SELECT b.user_id,
       p.product_id,
       NOW() - make_interval(days => ((b.rn + p.pick) % 20)::int)
  FROM buyers b
  CROSS JOIN LATERAL (
      SELECT product_id,
             ROW_NUMBER() OVER (ORDER BY md5(b.user_id || product_id)) AS pick
        FROM products
       WHERE status = 'PUBLISHED'
       ORDER BY md5(b.user_id || product_id)
       LIMIT 5
  ) p
ON CONFLICT (user_id, product_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 15
)
INSERT INTO stock_alerts (alert_id, user_id, product_id, sku_id, created_at, notified_at)
SELECT 'SA_DEMO_' || lpad(ROW_NUMBER() OVER (ORDER BY b.rn, v.sku_id)::text, 5, '0'),
       b.user_id,
       v.product_id,
       v.sku_id,
       NOW() - make_interval(days => (b.rn % 10)::int),
       CASE WHEN (b.rn % 3) = 0
             THEN NOW() - make_interval(days => (b.rn % 4)::int) END
  FROM buyers b
  CROSS JOIN LATERAL (
      SELECT p.product_id, v.sku_id
        FROM products p
        JOIN product_variants v ON v.product_id = p.product_id
       WHERE p.status = 'PUBLISHED'
       ORDER BY md5(b.user_id || v.sku_id)
       LIMIT 2
  ) v
ON CONFLICT (user_id, sku_id) DO NOTHING;

WITH picked AS (
    SELECT sku_id, warehouse_id,
           ROW_NUMBER() OVER (ORDER BY md5(sku_id || warehouse_id)) AS rn
      FROM inventory_items
     WHERE available_qty > 0
     ORDER BY md5(sku_id || warehouse_id)
     LIMIT 40
)
INSERT INTO stock_adjustments (adj_id, sku_id, warehouse_id, qty, type, reason, order_id, occurred_at)
SELECT 'ADJ_DEMO_' || lpad(p.rn::text, 5, '0'),
       p.sku_id,
       p.warehouse_id,
       (p.rn % 15) + 1,
       CASE p.rn % 5
           WHEN 0 THEN 'MANUAL_INCREASE'
           WHEN 1 THEN 'MANUAL_DECREASE'
           WHEN 2 THEN 'DAMAGED'
           WHEN 3 THEN 'TRANSFER_IN'
           ELSE 'TRANSFER_OUT'
       END,
       CASE p.rn % 5
           WHEN 0 THEN 'Nhập thêm hàng từ nhà cung cấp'
           WHEN 1 THEN 'Điều chỉnh kiểm kê định kỳ'
           WHEN 2 THEN 'Hàng hỏng trong quá trình bảo quản'
           WHEN 3 THEN 'Nhận hàng chuyển từ kho phụ'
           ELSE 'Chuyển hàng sang kho phụ'
       END,
       NULL,
       NOW() - make_interval(days => (p.rn % 60)::int)
  FROM picked p
ON CONFLICT (adj_id) DO NOTHING;

WITH picked_inventory AS (
    SELECT sku_id, warehouse_id,
           ROW_NUMBER() OVER (ORDER BY md5(sku_id || warehouse_id)) AS rn
      FROM inventory_items
     WHERE available_qty > 0
     ORDER BY md5(sku_id || warehouse_id)
     LIMIT 20
),
picked_orders AS (
    SELECT order_id,
           ROW_NUMBER() OVER (ORDER BY order_id) AS rn
      FROM orders
     WHERE status = 'COMPLETED'
     ORDER BY order_id
     LIMIT 20
)
INSERT INTO stock_reservations
    (reservation_id, sku_id, warehouse_id, order_id, qty, status,
     reserved_at, expires_at, decided_at, version)
SELECT 'RSV_DEMO_' || lpad(i.rn::text, 5, '0'),
       i.sku_id,
       i.warehouse_id,
       o.order_id,
       1 + (i.rn % 3),
       CASE
           WHEN i.rn <= 6 THEN 'RESERVED'
           WHEN i.rn <= 11 THEN 'COMMITTED'
           WHEN i.rn <= 16 THEN 'RELEASED'
           ELSE 'FAILED'
       END,
       NOW() - make_interval(hours => (i.rn * 2)::int),
       CASE WHEN i.rn <= 6 THEN NOW() + INTERVAL '1 hour' END,
       CASE WHEN i.rn > 6 THEN NOW() - make_interval(hours => i.rn::int) END,
       0
  FROM picked_inventory i
  JOIN picked_orders o ON o.rn = i.rn
ON CONFLICT (reservation_id) DO NOTHING;

WITH picked AS (
    SELECT merchant_id,
           ROW_NUMBER() OVER (ORDER BY merchant_id) AS rn
      FROM merchants
     ORDER BY merchant_id
     LIMIT 5
)
INSERT INTO warehouses
    (warehouse_id, merchant_id, address, priority_level, status, version, created_at, updated_at)
SELECT 'WH_DEMO_' || lpad(p.rn::text, 3, '0'),
       p.merchant_id,
       'Kho phụ demo số ' || p.rn || ' - TP. Hồ Chí Minh',
       2,
       'ACTIVE',
       0,
       TIMESTAMPTZ '2025-03-01 00:00:00+00',
       TIMESTAMPTZ '2025-03-01 00:00:00+00'
  FROM picked p
ON CONFLICT (warehouse_id) DO NOTHING;

WITH picked AS (
    SELECT m.merchant_id,
           m.rn,
           (SELECT w.warehouse_id FROM warehouses w
             WHERE w.merchant_id = m.merchant_id
             ORDER BY w.priority_level, w.warehouse_id
             LIMIT 1) AS from_wh
      FROM (
          SELECT merchant_id,
                 ROW_NUMBER() OVER (ORDER BY merchant_id) AS rn
            FROM merchants
           ORDER BY merchant_id
           LIMIT 5
      ) m
),
slots AS (
    SELECT generate_series(0, 2) AS slot
)
INSERT INTO stock_transfers
    (transfer_id, merchant_id, from_warehouse_id, to_warehouse_id, sku_id, qty,
     status, initiated_at, completed_at, cancelled_at, version)
SELECT 'TRF_DEMO_' || lpad(((p.rn - 1) * 3 + s.slot + 1)::text, 5, '0'),
       p.merchant_id,
       p.from_wh,
       'WH_DEMO_' || lpad(p.rn::text, 3, '0'),
       (SELECT ii.sku_id FROM inventory_items ii
         WHERE ii.warehouse_id = p.from_wh AND ii.available_qty > 0
         ORDER BY ii.sku_id LIMIT 1),
       (s.slot % 4) + 2,
       CASE s.slot
           WHEN 0 THEN 'INITIATED'
           WHEN 1 THEN 'COMPLETED'
           ELSE 'CANCELLED'
       END,
       NOW() - make_interval(days => ((p.rn + s.slot) % 20)::int),
       CASE WHEN s.slot = 1 THEN NOW() - make_interval(days => (p.rn % 7)::int) END,
       CASE WHEN s.slot = 2 THEN NOW() - make_interval(days => (p.rn % 5)::int) END,
       0
  FROM picked p
  CROSS JOIN slots s
 WHERE p.from_wh IS NOT NULL
   AND EXISTS (
       SELECT 1 FROM inventory_items ii
        WHERE ii.warehouse_id = p.from_wh AND ii.available_qty > 0)
ON CONFLICT (transfer_id) DO NOTHING;

WITH picked AS (
    SELECT noti_id,
           ROW_NUMBER() OVER (ORDER BY noti_id) AS rn
      FROM notifications
     WHERE noti_id LIKE 'NOTI_ORD_%'
     ORDER BY noti_id
     LIMIT 10
),
attempts AS (
    SELECT noti_id, rn, 0 AS attempt_number,
           CASE WHEN rn <= 3 THEN 'FAILED' ELSE 'SUCCEEDED' END AS status
      FROM picked
    UNION ALL
    SELECT noti_id, rn, 1 AS attempt_number, 'SUCCEEDED' AS status
      FROM picked
     WHERE rn <= 3
)
INSERT INTO notification_delivery_attempts
    (attempt_id, notification_id, channel, attempt_number, status,
     provider_message_id, error, started_at, completed_at, updated_at)
SELECT a.noti_id || ':IN_APP:' || a.attempt_number,
       a.noti_id,
       'IN_APP',
       a.attempt_number,
       a.status,
       CASE WHEN a.status = 'SUCCEEDED' THEN 'inapp-' || a.noti_id || '-' || a.attempt_number END,
       CASE WHEN a.status = 'FAILED' THEN 'PROVIDER_TIMEOUT' END,
       NOW() - make_interval(days => (a.rn % 5)::int, mins => (a.attempt_number * 2)::int),
       NOW() - make_interval(days => (a.rn % 5)::int, mins => (a.attempt_number * 2)::int),
       NOW() - make_interval(days => (a.rn % 5)::int)
  FROM attempts a
ON CONFLICT (notification_id, attempt_number) DO NOTHING;

INSERT INTO data_export_requests
    (export_request_id, user_id, status, requested_at, file_url, completed_at)
SELECT '01M9EXP' || lpad('1', 19, '0'),
       u.user_id,
       'COMPLETED',
       NOW() - INTERVAL '12 days',
       '/demo/exports/buyer-001.json',
       NOW() - INTERVAL '12 days' + INTERVAL '2 minutes'
  FROM users u
 WHERE u.email = 'buyer_001@example.test'
ON CONFLICT (export_request_id) DO NOTHING;

INSERT INTO account_deletion_requests
    (deletion_request_id, user_id, status, requested_at,
     scheduled_deletion_at, canceled_at, completed_at)
SELECT '01M9DEL' || lpad('1', 19, '0'),
       u.user_id,
       'CANCELLED',
       NOW() - INTERVAL '20 days',
       NOW() - INTERVAL '6 days',
       NOW() - INTERVAL '19 days',
       NULL
  FROM users u
 WHERE u.email = 'buyer_002@example.test'
ON CONFLICT (deletion_request_id) DO NOTHING;
