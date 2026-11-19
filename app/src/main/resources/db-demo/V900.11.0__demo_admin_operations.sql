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


CREATE TEMP TABLE demo_return_seed AS
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
)
SELECT 'RET_DEMO_' || lpad(p.rn::text, 5, '0') AS return_id,
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
 WHERE p.rn <= 16;

INSERT INTO order_returns
    (return_id, order_id, user_id, merchant_id, reason, evidence_url,
     refund_amount, refund_currency, return_warehouse_id, item_condition,
     rejection_reason, status, version, requested_at, decided_at, received_at,
     refund_status, refund_attempts, refund_failure_reason,
     next_refund_attempt_at, updated_at)
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
       NULL,
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
       NOW()
  FROM demo_return_seed s
ON CONFLICT (return_id) DO NOTHING;

INSERT INTO order_return_items (return_id, sku_id, qty, unit_price, refund_amount)
SELECT s.return_id, s.sku_id, s.qty, s.unit_price, s.qty * s.unit_price
  FROM demo_return_seed s
ON CONFLICT DO NOTHING;

DROP TABLE demo_return_seed;


WITH seeded AS (
    SELECT m.merchant_id,
           m.name,
           b.available,
           ROW_NUMBER() OVER (ORDER BY m.merchant_id) AS rn
      FROM merchants m
      JOIN merchant_balances b ON b.merchant_id = m.merchant_id AND b.currency = 'VND'
)
INSERT INTO merchant_payouts
    (payout_id, merchant_id, amount, currency, status, bank_name,
     bank_account_no, bank_account_name, external_ref, note,
     requested_at, completed_at, failed_at, failure_reason, version)
SELECT 'PAY_DEMO_' || lpad(s.rn::text, 5, '0'),
       s.merchant_id,
       LEAST(GREATEST(ROUND(s.available * 0.02), 1000000), 50000000),
       'VND',
       CASE
           WHEN s.rn <= 10 THEN 'PENDING'
           WHEN s.rn <= 16 THEN 'PROCESSING'
           WHEN s.rn <= 46 THEN 'COMPLETED'
           ELSE 'FAILED'
       END,
       CASE (s.rn % 4)
           WHEN 0 THEN 'Vietcombank'
           WHEN 1 THEN 'Techcombank'
           WHEN 2 THEN 'MB Bank'
           ELSE 'ACB'
       END,
       '00' || lpad((100000000 + s.rn)::text, 10, '0'),
       s.name,
       CASE WHEN s.rn <= 46 AND s.rn > 16 THEN 'VCB-' || lpad(s.rn::text, 8, '0') END,
       'Rút tiền định kỳ cho gian hàng ' || s.name,
       NOW() - make_interval(days => ((s.rn % 20) + 1)::int),
       CASE WHEN s.rn > 16 AND s.rn <= 46
             THEN NOW() - make_interval(days => (s.rn % 10)::int) END,
       CASE WHEN s.rn > 46
             THEN NOW() - make_interval(days => (s.rn % 5)::int) END,
       CASE WHEN s.rn > 46 THEN 'Tài khoản ngân hàng không khớp tên chủ sở hữu' END,
       0
  FROM seeded s
ON CONFLICT (payout_id) DO NOTHING;

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

WITH buyers AS (
    SELECT u.user_id, u.email,
           ROW_NUMBER() OVER (ORDER BY u.user_id) AS rn
      FROM users u
      JOIN user_roles r ON r.user_id = u.user_id AND r.role = 'BUYER'
)
INSERT INTO user_feedbacks
    (feedback_id, user_id, category, subject, content, rating,
     contact_email, contact_phone, status, handled_by, handled_at,
     created_at, updated_at, admin_reply)
SELECT 'FB_DEMO_' || lpad(b.rn::text, 5, '0'),
       b.user_id,
       CASE b.rn % 5
           WHEN 0 THEN 'COMPLAINT'
           WHEN 1 THEN 'BUG'
           WHEN 2 THEN 'FEATURE_REQUEST'
           WHEN 3 THEN 'CONTACT'
           ELSE 'GENERAL'
       END,
       CASE b.rn % 5
           WHEN 0 THEN 'Đơn hàng giao chậm hơn dự kiến'
           WHEN 1 THEN 'Không áp dụng được mã giảm giá'
           WHEN 2 THEN 'Đề xuất thêm bộ lọc theo thương hiệu'
           WHEN 3 THEN 'Cần hỗ trợ đổi địa chỉ nhận hàng'
           ELSE 'Góp ý trải nghiệm tìm kiếm sản phẩm'
       END,
       'Nội dung góp ý chi tiết từ khách hàng, kèm thông tin đơn hàng và mong muốn được hỗ trợ thêm.',
       (b.rn % 5) + 1,
       b.email,
       NULL,
       CASE
           WHEN b.rn <= 6 THEN 'OPEN'
           WHEN b.rn <= 10 THEN 'IN_REVIEW'
           WHEN b.rn <= 16 THEN 'RESOLVED'
           ELSE 'CLOSED'
       END,
       CASE WHEN b.rn > 6
             THEN (SELECT user_id FROM user_roles WHERE role = 'CS_ADMIN' ORDER BY user_id LIMIT 1) END,
       CASE WHEN b.rn > 6 THEN NOW() - make_interval(days => (b.rn % 7)::int) END,
       NOW() - make_interval(days => ((b.rn % 20) + 1)::int),
       NOW() - make_interval(days => (b.rn % 5)::int),
       CASE WHEN b.rn > 10
             THEN 'Cảm ơn bạn đã phản hồi. Bộ phận CSKH đã kiểm tra và xử lý xong yêu cầu này.' END
  FROM buyers b
 WHERE b.rn <= 20
ON CONFLICT (feedback_id) DO NOTHING;

INSERT INTO attribute_templates
    (template_id, category_id, attributes, created_at, updated_at, version)
SELECT 'AT_' || c.category_id,
       c.category_id,
       CASE
           WHEN c.name ILIKE '%thời trang%' OR c.name ILIKE '%giày%'
                OR c.name ILIKE '%túi%' OR c.name ILIKE '%trang sức%'
               THEN jsonb_build_object('size', true, 'color', true, 'material', true)
           WHEN c.name ILIKE '%điện tử%' OR c.name ILIKE '%máy tính%'
                OR c.name ILIKE '%điện thoại%' OR c.name ILIKE '%âm thanh%'
               THEN jsonb_build_object('capacity', true, 'voltage', true, 'warranty', true)
           WHEN c.name ILIKE '%sức khỏe%' OR c.name ILIKE '%làm đẹp%'
                OR c.name ILIKE '%chăm sóc da%' OR c.name ILIKE '%trang điểm%'
               THEN jsonb_build_object('skinType', true, 'volume', true, 'origin', true)
           WHEN c.name ILIKE '%nhà%' OR c.name ILIKE '%bếp%'
               THEN jsonb_build_object('material', true, 'dimensions', true, 'color', true)
           ELSE jsonb_build_object('color', true, 'material', true, 'size', true)
       END,
       NOW(), NOW(), 0
  FROM categories c
ON CONFLICT (category_id) DO NOTHING;

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
     merchant_id, version, created_at, updated_at)
SELECT t.prefix || '-' || lpad(m.rn::text, 3, '0'),
       NULL,
       t.discount_amount,
       'VND',
       200,
       20,
       0,
       NOW() - INTERVAL '7 days',
       TIMESTAMPTZ '2027-01-31 22:00:00+07',
       'SHOP',
       t.type,
       m.merchant_id,
       0,
       NOW() - INTERVAL '7 days',
       NOW()
  FROM merchants_ranked m
  CROSS JOIN templates t
ON CONFLICT (voucher_code) DO NOTHING;
