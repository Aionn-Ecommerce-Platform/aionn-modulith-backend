WITH consent_types(consent_type, marketing) AS (
    VALUES ('TERMS_OF_SERVICE', false),
           ('PRIVACY_POLICY', false),
           ('MARKETING', true)
)
INSERT INTO user_consents
    (consent_id, user_id, consent_type, version, agreed_at, revoked_at, ip_address)
SELECT '01M9CON' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id, c.consent_type)::text, 19, '0'),
       u.user_id,
       c.consent_type,
       '1.0',
       NOW() - make_interval(days => (abs(hashtext(u.user_id)) % 180)::int),
       CASE WHEN c.marketing AND (abs(hashtext(u.user_id)) % 4 = 0)
             THEN NOW() - make_interval(days => (abs(hashtext(u.user_id)) % 30)::int) END,
       '203.113.1.' || (abs(hashtext(u.user_id)) % 250)
  FROM users u
  CROSS JOIN consent_types c
 WHERE NOT EXISTS (
       SELECT 1 FROM user_consents existing
        WHERE existing.user_id = u.user_id
          AND existing.consent_type = c.consent_type);


WITH translations(name, en_name) AS (
    VALUES ('Điện tử', 'Electronics'),
           ('Máy tính xách tay', 'Laptops'),
           ('Điện thoại thông minh', 'Smartphones'),
           ('Thiết bị nhà bếp', 'Kitchen Appliances'),
           ('Thời trang', 'Fashion'),
           ('Giày dép', 'Shoes'),
           ('Thời trang nữ', 'Women''s Fashion'),
           ('Phụ kiện công nghệ', 'Tech Accessories'),
           ('Chăm sóc da', 'Skincare'),
           ('Nội thất', 'Furniture'),
           ('Sức khỏe & Làm đẹp', 'Health & Beauty'),
           ('Nhà cửa & Bếp', 'Home & Kitchen'),
           ('Điều hòa nhiệt độ', 'Climate Control'),
           ('Nhà thông minh', 'Smart Home'),
           ('Chiếu sáng thông minh', 'Smart Lighting'),
           ('Thời trang nam', 'Men''s Fashion'),
           ('Vitamin & Thực phẩm chức năng', 'Vitamins & Supplements'),
           ('An ninh thông minh', 'Smart Security'),
           ('Thể thao & Dã ngoại', 'Sports & Outdoors'),
           ('Ô tô & Xe máy', 'Cars & Motorbikes'),
           ('Máy tính bảng', 'Tablets'),
           ('Âm thanh', 'Audio'),
           ('Thiết bị đeo thông minh', 'Smart Wearables'),
           ('Đồng hồ', 'Watches'),
           ('Kính râm', 'Sunglasses'),
           ('Túi xách', 'Handbags'),
           ('Trang sức', 'Jewelry'),
           ('Trang phục thể thao', 'Sportswear'),
           ('Trang trí nhà cửa', 'Home Decor'),
           ('Thực phẩm', 'Groceries'),
           ('Đồ uống', 'Beverages'),
           ('Dụng cụ nhà bếp', 'Kitchen Tools'),
           ('Trang điểm', 'Makeup'),
           ('Nước hoa', 'Fragrance'),
           ('Chăm sóc tóc', 'Hair Care'),
           ('Dụng cụ thể thao', 'Sports Equipment'),
           ('Thiết bị tập luyện', 'Fitness Equipment'),
           ('Đồ dã ngoại', 'Camping Gear'),
           ('Ô tô', 'Cars'),
           ('Xe máy', 'Motorbikes')
)
INSERT INTO category_translations (category_id, locale, name, created_at, updated_at)
SELECT c.category_id, 'en-US', COALESCE(t.en_name, c.name), NOW(), NOW()
  FROM categories c
  LEFT JOIN translations t ON t.name = c.name
ON CONFLICT (category_id, locale) DO NOTHING;

INSERT INTO brand_translations (brand_id, locale, name, description, created_at, updated_at)
SELECT b.brand_id, 'en-US', b.name,
       COALESCE(b.description, 'Official ' || b.name || ' products on Aionn.'),
       NOW(), NOW()
  FROM brands b
ON CONFLICT (brand_id, locale) DO NOTHING;

INSERT INTO product_translations (product_id, locale, name, ai_description, created_at, updated_at)
SELECT p.product_id, 'en-US', p.name, p.ai_description, NOW(), NOW()
  FROM products p
 WHERE p.status = 'PUBLISHED'
 ORDER BY md5(p.product_id)
 LIMIT 1000
ON CONFLICT (product_id, locale) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 10
)
INSERT INTO agent_identities
    (agent_id, owner_id, name, key_hash, permissions, status, expiry_at, created_at)
SELECT '01M9AGT' || lpad(b.rn::text, 19, '0'),
       b.user_id,
       'Shopping assistant ' || b.rn,
       'agent-demo-hash-' || lpad(b.rn::text, 6, '0'),
       '["catalog:read","orders:read","cart:write"]'::jsonb,
       CASE WHEN b.rn = 10 THEN 'SUSPENDED' ELSE 'ACTIVE' END,
       NOW() + INTERVAL '1 year',
       NOW() - make_interval(days => (b.rn * 3)::int)
  FROM buyers b
ON CONFLICT (agent_id) DO NOTHING;

WITH picked AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM users
     ORDER BY user_id
     LIMIT 5
),
codes AS (
    SELECT generate_series(1, 5) AS code_no
)
INSERT INTO backup_codes (backup_code_id, user_id, code_hash, generated_at, used_at)
SELECT '01M9BC' || lpad(ROW_NUMBER() OVER (ORDER BY p.rn, c.code_no)::text, 19, '0'),
       p.user_id,
       'bc-demo-hash-' || p.rn || '-' || c.code_no,
       NOW() - make_interval(days => (p.rn * 2)::int),
       CASE WHEN c.code_no = 5 THEN NOW() - make_interval(days => p.rn::int) END
  FROM picked p
  CROSS JOIN codes c
ON CONFLICT (backup_code_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 20
)
INSERT INTO device_tokens
    (token_id, user_id, device_token, os, is_active, version, registered_at, updated_at)
SELECT 'DT_DEMO_' || lpad(b.rn::text, 5, '0'),
       b.user_id,
       'fcm-demo-token-' || b.rn || '-aionn',
       CASE WHEN b.rn % 2 = 0 THEN 'ANDROID' ELSE 'IOS' END,
       true,
       0,
       NOW() - make_interval(days => (b.rn % 30)::int),
       NOW() - make_interval(days => (b.rn % 7)::int)
  FROM buyers b
ON CONFLICT (token_id) DO NOTHING;

WITH buyers AS (
    SELECT user_id,
           ROW_NUMBER() OVER (ORDER BY user_id) AS rn
      FROM user_roles
     WHERE role = 'BUYER'
     ORDER BY user_id
     LIMIT 20
)
INSERT INTO social_accounts
    (social_account_id, user_id, provider, provider_user_id, created_at)
SELECT '01M9SOC' || lpad(b.rn::text, 19, '0'),
       b.user_id,
       CASE WHEN b.rn % 2 = 0 THEN 'GOOGLE' ELSE 'FACEBOOK' END,
       'social-demo-' || b.rn,
       NOW() - make_interval(days => (b.rn * 4)::int)
  FROM buyers b
ON CONFLICT (social_account_id) DO NOTHING;

INSERT INTO chat_user_blocks
    (block_id, blocker_id, blocked_id, reason, is_active, version, created_at, updated_at)
SELECT 'BLK_DEMO_' || lpad(v.rn::text, 5, '0'),
       blocker.user_id,
       blocked.user_id,
       v.reason,
       true,
       0,
       NOW() - make_interval(days => v.rn::int),
       NOW() - make_interval(days => v.rn::int)
  FROM (VALUES
        (1, 'buyer_001@example.test', 'buyer_050@example.test', 'Spam tin nhắn quảng cáo'),
        (2, 'buyer_002@example.test', 'buyer_051@example.test', 'Nội dung không phù hợp'),
        (3, 'buyer_003@example.test', 'buyer_052@example.test', 'Quấy rối trong chat'),
        (4, 'merchant_001@example.test', 'buyer_100@example.test', 'Khách gửi tin nhắn lặp lại'),
        (5, 'buyer_101@example.test', 'merchant_002@example.test', 'Tư vấn không mong muốn')
  ) AS v(rn, blocker_email, blocked_email, reason)
  JOIN users blocker ON blocker.email = v.blocker_email
  JOIN users blocked ON blocked.email = v.blocked_email
ON CONFLICT (block_id) DO NOTHING;

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


WITH picked AS (
    SELECT order_id, user_id,
           ROW_NUMBER() OVER (ORDER BY order_id) AS rn
      FROM orders
     ORDER BY order_id
     LIMIT 20
)
INSERT INTO order_placement_operations
    (user_id, idempotency_key, request_hash, order_id, status, created_at, updated_at)
SELECT p.user_id,
       'IDEM_DEMO_' || lpad(p.rn::text, 4, '0'),
       md5('request-' || p.rn) || md5('payload-' || p.rn),
       p.order_id,
       'COMPLETED',
       NOW() - make_interval(days => (p.rn % 30)::int),
       NOW() - make_interval(days => (p.rn % 30)::int)
  FROM picked p
ON CONFLICT (user_id, idempotency_key) DO NOTHING;

WITH picked AS (
    SELECT order_id, user_id,
           ROW_NUMBER() OVER (ORDER BY order_id) AS rn
      FROM orders
     ORDER BY order_id
     LIMIT 12
)
INSERT INTO ordering_compensation_tasks
    (task_id, task_type, resource_id, user_id, order_id, reason, status,
     attempts, next_attempt_at, last_error, completed_at, created_at, updated_at)
SELECT 'TASK_DEMO_' || lpad(p.rn::text, 4, '0'),
       CASE p.rn % 6
           WHEN 0 THEN 'VOUCHER_RELEASE'
           WHEN 1 THEN 'RESERVATION_RELEASE'
           WHEN 2 THEN 'FLASH_SALE_RELEASE'
           WHEN 3 THEN 'REFUND'
           WHEN 4 THEN 'STOCK_COMMIT'
           ELSE 'SHIPMENT_REGISTER'
       END,
       p.order_id,
       p.user_id,
       p.order_id,
       'Xử lý bù sau sự kiện đơn hàng thất bại',
       CASE
           WHEN p.rn <= 4 THEN 'COMPLETED'
           WHEN p.rn <= 8 THEN 'PENDING'
           WHEN p.rn <= 10 THEN 'FAILED'
           ELSE 'DEAD_LETTER'
       END,
       CASE WHEN p.rn <= 4 THEN 1 WHEN p.rn <= 8 THEN 0 ELSE 5 END,
       NOW() + make_interval(mins => (p.rn * 3)::int),
       CASE WHEN p.rn > 8 THEN 'Provider timeout after retries' END,
       CASE WHEN p.rn <= 4 THEN NOW() - make_interval(days => (p.rn % 5)::int) END,
       NOW() - make_interval(days => (p.rn % 15)::int),
       NOW() - make_interval(days => (p.rn % 5)::int)
  FROM picked p
ON CONFLICT (task_id) DO NOTHING;

INSERT INTO processed_carrier_webhooks (event_id, processed_at)
SELECT 'CARRIER_WEBHOOK_DEMO_' || lpad(gs::text, 5, '0'),
       NOW() - make_interval(hours => (gs * 2)::int)
  FROM generate_series(1, 20) AS gs
ON CONFLICT (event_id) DO NOTHING;

INSERT INTO recommendation_erased_users (user_id, erased_at)
SELECT u.user_id, NOW() - make_interval(days => (u.rn * 5)::int)
  FROM (
      SELECT user_id,
             ROW_NUMBER() OVER (ORDER BY user_id) AS rn
        FROM users
       ORDER BY user_id
       OFFSET 500 LIMIT 3
  ) u
ON CONFLICT (user_id) DO NOTHING;

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
