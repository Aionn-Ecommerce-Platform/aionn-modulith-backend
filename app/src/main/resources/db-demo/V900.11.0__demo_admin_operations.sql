-- ===== V900.11.0__demo_admin_operations.sql =====
UPDATE products
   SET status = 'PENDING_REVIEW',
       updated_at = NOW()
 WHERE product_id IN (
       SELECT p.product_id
         FROM products p
        WHERE p.status = 'PUBLISHED'
          AND EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.product_id)
          AND NOT EXISTS (
              SELECT 1 FROM flash_sale_registrations r WHERE r.product_id = p.product_id)
        ORDER BY md5(p.product_id)
        LIMIT 12);

CREATE TEMP TABLE demo_kyc_seed AS
WITH owners AS (
    SELECT u.user_id,
           ROW_NUMBER() OVER (ORDER BY m.name, m.merchant_id) AS rn
      FROM merchants m
      JOIN users u ON u.user_id = m.owner_id
)
SELECT '01M9KYC' || lpad(o.rn::text, 19, '0') AS kyc_id,
       o.user_id,
       o.rn,
       CASE
           WHEN o.rn <= 4 THEN 'SUBMITTED'
           WHEN o.rn <= 7 THEN 'IN_REVIEW'
           WHEN o.rn <= 10 THEN 'APPROVED'
           ELSE 'REJECTED'
       END AS status,
       CASE WHEN o.rn % 3 = 0 THEN 'PASSPORT' ELSE 'CCCD' END AS doc_type
  FROM owners o
 WHERE o.rn <= 12;

INSERT INTO kyc_profiles
    (kyc_id, user_id, doc_type, blob_url, status, reviewer_id, review_note,
     decision_admin_id, reject_reason, submitted_at, approved_at, expired_at,
     created_at, provider, version)
SELECT s.kyc_id,
       s.user_id,
       s.doc_type,
       '/images/logo.png',
       s.status,
       CASE WHEN s.status <> 'SUBMITTED'
             THEN (SELECT user_id FROM user_roles WHERE role = 'CS_ADMIN' ORDER BY user_id LIMIT 1) END,
       CASE WHEN s.status = 'APPROVED' THEN 'Giấy tờ hợp lệ, đã đối chiếu thông tin.' END,
       CASE WHEN s.status IN ('APPROVED', 'REJECTED')
             THEN (SELECT user_id FROM user_roles WHERE role = 'CS_ADMIN' ORDER BY user_id LIMIT 1) END,
       CASE WHEN s.status = 'REJECTED' THEN 'Ảnh giấy tờ bị mờ, không đọc được số định danh.' END,
       NOW() - make_interval(days => s.rn::int),
       CASE WHEN s.status = 'APPROVED' THEN NOW() - make_interval(days => (s.rn - 1)::int) END,
       NULL,
       NOW() - make_interval(days => (s.rn + 2)::int),
       NULL,
       0
  FROM demo_kyc_seed s
ON CONFLICT (kyc_id) DO NOTHING;

INSERT INTO kyc_documents
    (document_id, kyc_id, document_type, url, public_id, status, uploaded_at)
SELECT '01M9KYCDOC' || lpad(ROW_NUMBER() OVER (ORDER BY s.kyc_id, d.doc_type)::text, 16, '0'),
       s.kyc_id,
       d.doc_type,
       '/images/logo.png',
       NULL,
       CASE WHEN s.status = 'APPROVED' THEN 'ACCEPTED'
            WHEN s.status = 'REJECTED' THEN 'REJECTED'
            ELSE 'UPLOADED' END,
       NOW() - make_interval(days => s.rn::int)
  FROM demo_kyc_seed s
  CROSS JOIN (VALUES ('ID_FRONT'), ('ID_BACK'), ('SELFIE')) AS d(doc_type)
ON CONFLICT (document_id) DO NOTHING;

DROP TABLE demo_kyc_seed;

-- ===== V900.11.2__demo_member_inventory_activity.sql =====
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

-- ===== V900.11.3__demo_remaining_tables.sql =====
WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 15
)
INSERT INTO carts (cart_id, user_id, voucher_code, version, created_at, updated_at)
SELECT 'CART_DEMO_' || lpad(b.rn::text, 4, '0'),
       b.user_id,
       CASE WHEN b.rn = 1 THEN 'SHOP10-001' END,
       0,
       NOW() - make_interval(days => (b.rn % 10)::int),
       NOW() - make_interval(hours => b.rn::int)
  FROM buyers b
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO cart_items (cart_id, sku_id, qty)
SELECT c.cart_id, v.sku_id, 1 + (v.pick % 3)
  FROM carts c
  JOIN users u ON u.user_id = c.user_id
  CROSS JOIN LATERAL (
      SELECT pv.sku_id,
             ROW_NUMBER() OVER (ORDER BY md5(c.cart_id || pv.sku_id)) AS pick
        FROM product_variants pv
        JOIN products p ON p.product_id = pv.product_id
       WHERE p.status = 'PUBLISHED'
       ORDER BY md5(c.cart_id || pv.sku_id)
       LIMIT 3
  ) v
 WHERE c.cart_id LIKE 'CART_DEMO_%'
    OR u.email = 'buyer_001@example.test'
ON CONFLICT (cart_id, sku_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 10
)
INSERT INTO ucp_identity_links
    (link_id, platform_id, platform_subject, customer_id, status, scopes,
     linked_at, expires_at, metadata)
SELECT 'UCPLINK_' || lpad(b.rn::text, 5, '0'),
       CASE WHEN b.rn % 2 = 0 THEN 'shopify' ELSE 'tiktok-shop' END,
       'platform-subject-' || b.rn,
       b.user_id,
       'ACTIVE',
       '["checkout:write","orders:read"]'::jsonb,
       NOW() - make_interval(days => (b.rn * 3)::int),
       NOW() + INTERVAL '6 months',
       jsonb_build_object('source', 'demo', 'sequence', b.rn)
  FROM buyers b
ON CONFLICT (link_id) DO NOTHING;

WITH picked AS (
    SELECT u.user_id,
           u.rn,
           v.sku_id,
           o.order_id,
           c.cart_id
      FROM (
          SELECT u.user_id,
                 ROW_NUMBER() OVER (ORDER BY u.user_id) AS rn
            FROM users u
            JOIN user_roles r ON r.user_id = u.user_id AND r.role = 'BUYER'
            ORDER BY u.user_id
            LIMIT 10
      ) u
      LEFT JOIN LATERAL (
          SELECT pv.sku_id
            FROM product_variants pv
            JOIN products p ON p.product_id = pv.product_id
           WHERE p.status = 'PUBLISHED'
           ORDER BY md5(u.user_id || pv.sku_id)
           LIMIT 1
      ) v ON TRUE
      LEFT JOIN LATERAL (
          SELECT o.order_id
            FROM orders o
           WHERE o.user_id = u.user_id
           ORDER BY o.created_at DESC
           LIMIT 1
      ) o ON TRUE
      LEFT JOIN LATERAL (
          SELECT c.cart_id
            FROM carts c
           WHERE c.user_id = u.user_id
           LIMIT 1
      ) c ON TRUE
)
INSERT INTO ucp_checkout_sessions
    (id, user_id, cart_id, status, currency, items, buyer, context, order_id,
     created_at, updated_at, expires_at, price_snapshot, payment, fulfillment,
     voucher_code, version)
SELECT 'UCPSESS_' || lpad(p.rn::text, 5, '0'),
       p.user_id,
       p.cart_id,
       CASE p.rn % 3 WHEN 0 THEN 'completed' WHEN 1 THEN 'incomplete' ELSE 'canceled' END,
       'VND',
       jsonb_build_object(p.sku_id, 1),
       jsonb_build_object('email', 'buyer-' || p.rn || '@example.test', 'name', 'Demo Buyer ' || p.rn),
       jsonb_build_object('locale', 'vi-VN', 'source', 'ucp-demo'),
       CASE WHEN p.rn % 3 = 0 THEN p.order_id END,
       NOW() - make_interval(days => (p.rn % 6)::int),
       NOW() - make_interval(days => (p.rn % 2)::int),
       CASE WHEN p.rn % 3 = 1 THEN NOW() + INTERVAL '1 hour' END,
       jsonb_build_object('subtotal', 250000, 'shipping', 30000, 'total', 280000),
       CASE WHEN p.rn % 3 = 0
             THEN jsonb_build_object('gateway', 'STRIPE', 'status', 'PAID') END,
       CASE WHEN p.rn % 3 = 0
             THEN jsonb_build_object('method', 'STANDARD', 'status', 'DELIVERED') END,
       CASE WHEN p.rn % 3 = 0 THEN 'SHOP10-001' END,
       1
  FROM picked p
 WHERE p.sku_id IS NOT NULL
ON CONFLICT (id) DO NOTHING;

WITH picked AS (
    SELECT order_id,
           ROW_NUMBER() OVER (ORDER BY order_id) AS rn
      FROM orders
     ORDER BY order_id
     LIMIT 10
)
INSERT INTO ucp_webhook_deliveries
    (delivery_id, event_type, order_id, webhook_url, payload, status,
     attempts, next_attempt_at, last_error, created_at, updated_at)
SELECT 'UCPDEL_' || lpad(p.rn::text, 5, '0'),
       CASE WHEN p.rn % 2 = 0 THEN 'order.updated' ELSE 'order.created' END,
       p.order_id,
       'https://merchant.example.test/ucp/webhook',
       jsonb_build_object(
           'event_id', 'UCPDEL_' || lpad(p.rn::text, 5, '0'),
           'event_type', CASE WHEN p.rn % 2 = 0 THEN 'order.updated' ELSE 'order.created' END,
           'order_id', p.order_id,
           'occurred_at', to_char(NOW() - make_interval(days => (p.rn % 5)::int), 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
           'data', jsonb_build_object('sequence', p.rn)
       ),
       CASE WHEN p.rn <= 7 THEN 'DELIVERED' ELSE 'DEAD' END,
       CASE WHEN p.rn <= 7 THEN 1 ELSE 10 END,
       NOW(),
       CASE WHEN p.rn > 7 THEN 'Webhook endpoint returned 500 after max retries' END,
       NOW() - make_interval(days => (p.rn % 10)::int),
       NOW() - make_interval(days => (p.rn % 5)::int)
  FROM picked p
ON CONFLICT (delivery_id) DO NOTHING;

-- ===== V900.11.4__demo_inventory_and_compensation_consistency.sql =====
-- Keep demo reservations consistent with stock: reserved units are not available.
WITH reserved AS (
    SELECT sku_id, warehouse_id, SUM(qty)::int AS qty
      FROM stock_reservations
     WHERE reservation_id LIKE 'RSV_DEMO_%'
       AND status = 'RESERVED'
     GROUP BY sku_id, warehouse_id
)
UPDATE inventory_items AS i
   SET available_qty = GREATEST(i.available_qty - r.qty, 0),
       updated_at = NOW()
  FROM reserved r
 WHERE i.sku_id = r.sku_id
   AND i.warehouse_id = r.warehouse_id;
