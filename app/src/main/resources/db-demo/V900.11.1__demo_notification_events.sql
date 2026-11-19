
INSERT INTO notification_templates
    (template_id, event_type, channel, category, locale, subject, content,
     placeholders, version, is_active, created_at, updated_at)
VALUES
    ('ord_confirmed_in_vi', 'ordering.order-confirmed', 'IN_APP', 'ORDER', 'vi-VN',
     'Đơn hàng {{orderId}} đã được xác nhận',
     'Đơn hàng {{orderId}} đã được xác nhận và đang được người bán chuẩn bị. Tổng thanh toán: {{amount}}.',
     '["orderId","amount"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_confirmed_in_en', 'ordering.order-confirmed', 'IN_APP', 'ORDER', 'en-US',
     'Order {{orderId}} confirmed',
     'Order {{orderId}} has been confirmed and is being prepared. Total: {{amount}}.',
     '["orderId","amount"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_shipped_in_vi', 'ordering.order-shipped', 'IN_APP', 'ORDER', 'vi-VN',
     'Đơn hàng {{orderId}} đang được giao',
     'Đơn hàng {{orderId}} đã được bàn giao cho đơn vị vận chuyển và đang trên đường tới bạn.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_shipped_in_en', 'ordering.order-shipped', 'IN_APP', 'ORDER', 'en-US',
     'Order {{orderId}} is on the way',
     'Order {{orderId}} has been handed to the carrier and is on its way to you.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_delivered_in_vi', 'ordering.order-delivered', 'IN_APP', 'ORDER', 'vi-VN',
     'Đơn hàng {{orderId}} đã giao thành công',
     'Đơn hàng {{orderId}} đã được giao thành công. Hãy đánh giá sản phẩm để giúp người mua khác nhé.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_delivered_in_en', 'ordering.order-delivered', 'IN_APP', 'ORDER', 'en-US',
     'Order {{orderId}} delivered',
     'Order {{orderId}} was delivered successfully. Leave a review to help other shoppers.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_cancelled_in_vi', 'ordering.order-cancelled', 'IN_APP', 'ORDER', 'vi-VN',
     'Đơn hàng {{orderId}} đã bị hủy',
     'Đơn hàng {{orderId}} đã bị hủy. Nếu bạn đã thanh toán, khoản tiền sẽ được hoàn về phương thức ban đầu.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('ord_cancelled_in_en', 'ordering.order-cancelled', 'IN_APP', 'ORDER', 'en-US',
     'Order {{orderId}} cancelled',
     'Order {{orderId}} was cancelled. Any captured payment will be refunded to the original method.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('pay_succeeded_in_vi', 'payment.payment-succeeded', 'IN_APP', 'TRANSACTION', 'vi-VN',
     'Thanh toán đơn hàng {{orderId}} thành công',
     'Chúng tôi đã nhận được thanh toán {{amount}} cho đơn hàng {{orderId}}.',
     '["orderId","amount"]'::jsonb, 1, true, NOW(), NOW()),
    ('pay_succeeded_in_en', 'payment.payment-succeeded', 'IN_APP', 'TRANSACTION', 'en-US',
     'Payment for order {{orderId}} succeeded',
     'We received your payment of {{amount}} for order {{orderId}}.',
     '["orderId","amount"]'::jsonb, 1, true, NOW(), NOW()),
    ('pay_failed_in_vi', 'payment.payment-failed', 'IN_APP', 'TRANSACTION', 'vi-VN',
     'Thanh toán đơn hàng {{orderId}} thất bại',
     'Thanh toán cho đơn hàng {{orderId}} không thành công. Vui lòng thử lại hoặc chọn phương thức khác.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('pay_failed_in_en', 'payment.payment-failed', 'IN_APP', 'TRANSACTION', 'en-US',
     'Payment for order {{orderId}} failed',
     'The payment for order {{orderId}} could not be completed. Please retry or choose another method.',
     '["orderId"]'::jsonb, 1, true, NOW(), NOW()),
    ('system_announcement_in_vi', 'system.announcement', 'IN_APP', 'SYSTEM', 'vi-VN',
     'Aionn cập nhật chính sách mua sắm',
     'Từ hôm nay, Aionn áp dụng chính sách đổi trả trong 7 ngày và miễn phí vận chuyển cho đơn từ 499.000đ.',
     '[]'::jsonb, 1, true, NOW(), NOW()),
    ('system_announcement_in_en', 'system.announcement', 'IN_APP', 'SYSTEM', 'en-US',
     'Aionn shopping policy update',
     'Starting today, Aionn offers 7-day returns and free shipping on orders from 499,000 VND.',
     '[]'::jsonb, 1, true, NOW(), NOW()),
    ('system_maintenance_in_vi', 'system.maintenance', 'IN_APP', 'SYSTEM', 'vi-VN',
     'Bảo trì hệ thống định kỳ',
     'Aionn sẽ bảo trì hệ thống từ 02:00 đến 03:00 ngày mai. Một số chức năng thanh toán có thể tạm gián đoạn.',
     '[]'::jsonb, 1, true, NOW(), NOW()),
    ('system_maintenance_in_en', 'system.maintenance', 'IN_APP', 'SYSTEM', 'en-US',
     'Scheduled system maintenance',
     'Aionn will be under maintenance from 02:00 to 03:00 tomorrow. Some payment features may be unavailable.',
     '[]'::jsonb, 1, true, NOW(), NOW())
ON CONFLICT (event_type, channel, locale) DO NOTHING;

WITH picked AS (
    SELECT o.order_id,
           o.user_id,
           o.status,
           o.total_amount,
           o.currency,
           ROW_NUMBER() OVER (PARTITION BY o.status ORDER BY o.created_at DESC, o.order_id) AS rn
      FROM orders o
     WHERE o.status IN ('PENDING', 'APPROVED', 'PREPARING', 'SHIPPED', 'COMPLETED', 'CANCELLED')
),
selected AS (
    SELECT p.*,
           ROW_NUMBER() OVER (ORDER BY p.status, p.order_id) AS seq
      FROM picked p
     WHERE p.rn <= 4
)
INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_ORD_' || lpad(s.seq::text, 5, '0'),
       s.user_id,
       CASE s.status
           WHEN 'SHIPPED' THEN 'ord_shipped_in_vi'
           WHEN 'COMPLETED' THEN 'ord_delivered_in_vi'
           WHEN 'CANCELLED' THEN 'ord_cancelled_in_vi'
           ELSE 'ord_confirmed_in_vi'
       END,
       'IN_APP',
       'ORDER',
       'HIGH',
       CASE s.status
           WHEN 'SHIPPED' THEN 'Đơn hàng ' || s.order_id || ' đang được giao'
           WHEN 'COMPLETED' THEN 'Đơn hàng ' || s.order_id || ' đã giao thành công'
           WHEN 'CANCELLED' THEN 'Đơn hàng ' || s.order_id || ' đã bị hủy'
           ELSE 'Đơn hàng ' || s.order_id || ' đã được xác nhận'
       END,
       CASE s.status
           WHEN 'SHIPPED' THEN 'Đơn hàng ' || s.order_id || ' đã được bàn giao cho đơn vị vận chuyển và đang trên đường tới bạn.'
           WHEN 'COMPLETED' THEN 'Đơn hàng ' || s.order_id || ' đã được giao thành công. Hãy đánh giá sản phẩm để giúp người mua khác nhé.'
           WHEN 'CANCELLED' THEN 'Đơn hàng ' || s.order_id || ' đã bị hủy. Nếu bạn đã thanh toán, khoản tiền sẽ được hoàn về phương thức ban đầu.'
           ELSE 'Đơn hàng ' || s.order_id || ' đã được xác nhận và đang được người bán chuẩn bị. Tổng thanh toán: ' || to_char(s.total_amount, 'FM999G999G999D00') || ' ' || s.currency || '.'
       END,
       NULL,
       CASE WHEN s.seq % 3 = 0 THEN 'READ' ELSE 'SENT' END,
       0,
       0,
       NOW() - make_interval(hours => (s.seq * 3)::int),
       NOW() - make_interval(hours => (s.seq * 3)::int),
       NOW() - make_interval(hours => (s.seq * 3)::int),
       CASE WHEN s.seq % 3 = 0 THEN NOW() - make_interval(hours => (s.seq * 3 - 1)::int) END,
       NULL
  FROM selected s
ON CONFLICT (noti_id) DO NOTHING;

WITH picked AS (
    SELECT p.payment_id,
           p.order_id,
           p.user_id,
           p.amount,
           p.currency,
           p.status,
           ROW_NUMBER() OVER (PARTITION BY p.status ORDER BY p.created_at DESC, p.payment_id) AS rn
      FROM payments p
     WHERE p.status IN ('PAID', 'FAILED')
)
INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_PAY_' || CASE p.status WHEN 'PAID' THEN 'OK_' ELSE 'FAIL_' END || lpad(p.rn::text, 4, '0'),
       p.user_id,
       CASE p.status WHEN 'PAID' THEN 'pay_succeeded_in_vi' ELSE 'pay_failed_in_vi' END,
       'IN_APP',
       'TRANSACTION',
       'HIGH',
       CASE p.status
           WHEN 'PAID' THEN 'Thanh toán đơn hàng ' || p.order_id || ' thành công'
           ELSE 'Thanh toán đơn hàng ' || p.order_id || ' thất bại'
       END,
       CASE p.status
           WHEN 'PAID' THEN 'Chúng tôi đã nhận được thanh toán ' || to_char(p.amount, 'FM999G999G999D00') || ' ' || p.currency || ' cho đơn hàng ' || p.order_id || '.'
           ELSE 'Thanh toán cho đơn hàng ' || p.order_id || ' không thành công. Vui lòng thử lại hoặc chọn phương thức khác.'
       END,
       NULL,
       CASE WHEN p.rn % 2 = 0 THEN 'READ' ELSE 'SENT' END,
       0,
       0,
       NOW() - make_interval(hours => (p.rn * 4)::int),
       NOW() - make_interval(hours => (p.rn * 4)::int),
       NOW() - make_interval(hours => (p.rn * 4)::int),
       CASE WHEN p.rn % 2 = 0 THEN NOW() - make_interval(hours => (p.rn * 4 - 1)::int) END,
       NULL
  FROM picked p
 WHERE p.rn <= 5
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_SYSANN_' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id)::text, 6, '0'),
       u.user_id,
       'system_announcement_in_vi',
       'IN_APP',
       'SYSTEM',
       'NORMAL',
       'Aionn cập nhật chính sách mua sắm',
       'Từ hôm nay, Aionn áp dụng chính sách đổi trả trong 7 ngày và miễn phí vận chuyển cho đơn từ 499.000đ.',
       NULL,
       'SENT',
       0,
       0,
       NOW() - INTERVAL '2 days',
       NOW() - INTERVAL '2 days',
       NOW() - INTERVAL '2 days',
       NULL,
       NULL
  FROM users u
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_SYSMAINT_' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id)::text, 6, '0'),
       u.user_id,
       'system_maintenance_in_vi',
       'IN_APP',
       'SYSTEM',
       'NORMAL',
       'Bảo trì hệ thống định kỳ',
       'Aionn sẽ bảo trì hệ thống từ 02:00 đến 03:00 ngày mai. Một số chức năng thanh toán có thể tạm gián đoạn.',
       NULL,
       'READ',
       0,
       0,
       NOW() - INTERVAL '1 day',
       NOW() - INTERVAL '1 day',
       NOW() - INTERVAL '1 day',
       NOW() - INTERVAL '20 hours',
       NULL
  FROM users u
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_CHAT_' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id)::text, 5, '0'),
       u.user_id,
       'chat_msg_in_app_vi',
       'IN_APP',
       'CHAT',
       'NORMAL',
       'Tin nhắn mới từ Aionn Tech Store',
       'Người bán đã trả lời câu hỏi của bạn về sản phẩm. Mở cuộc trò chuyện để xem chi tiết.',
       NULL,
       'SENT',
       0,
       0,
       NOW() - make_interval(hours => 2),
       NOW() - make_interval(hours => 2),
       NOW() - make_interval(hours => 2),
       NULL,
       NULL
  FROM (SELECT user_id FROM user_roles WHERE role = 'BUYER' ORDER BY user_id LIMIT 10) u
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_IDN_' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id)::text, 5, '0'),
       u.user_id,
       'idn_pwd_changed_in_vi',
       'IN_APP',
       'SECURITY',
       'HIGH',
       'Mật khẩu tài khoản đã được thay đổi',
       'Mật khẩu tài khoản Aionn của bạn vừa được thay đổi. Nếu không phải bạn, hãy liên hệ CSKH ngay.',
       NULL,
       'READ',
       0,
       0,
       NOW() - make_interval(days => 1),
       NOW() - make_interval(days => 1),
       NOW() - make_interval(days => 1),
       NOW() - make_interval(hours => 20),
       NULL
  FROM (SELECT user_id FROM user_roles WHERE role = 'BUYER' ORDER BY user_id LIMIT 5) u
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_FOL_' || lpad(ROW_NUMBER() OVER (ORDER BY m.merchant_id)::text, 5, '0'),
       m.owner_id,
       'merchant_follower_in_vi',
       'IN_APP',
       'PROMOTION',
       'NORMAL',
       'Gian hàng của bạn có người theo dõi mới',
       'Một khách hàng vừa theo dõi gian hàng ' || m.name || '. Đăng sản phẩm mới để giữ chân người theo dõi nhé.',
       NULL,
       'SENT',
       0,
       0,
       NOW() - make_interval(hours => 5),
       NOW() - make_interval(hours => 5),
       NOW() - make_interval(hours => 5),
       NULL,
       NULL
  FROM (SELECT merchant_id, owner_id, name FROM merchants ORDER BY merchant_id LIMIT 5) m
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_STOCK_' || lpad(ROW_NUMBER() OVER (ORDER BY u.user_id)::text, 5, '0'),
       u.user_id,
       'product_back_in_stock_in_vi',
       'IN_APP',
       'PROMOTION',
       'NORMAL',
       'Sản phẩm bạn chờ đã có hàng trở lại',
       'Sản phẩm trong danh sách chờ của bạn đã được nhập thêm hàng. Nhanh tay đặt trước khi hết hàng.',
       NULL,
       CASE WHEN ROW_NUMBER() OVER (ORDER BY u.user_id) % 2 = 0 THEN 'READ' ELSE 'SENT' END,
       0,
       0,
       NOW() - make_interval(hours => 6),
       NOW() - make_interval(hours => 6),
       NOW() - make_interval(hours => 6),
       CASE WHEN ROW_NUMBER() OVER (ORDER BY u.user_id) % 2 = 0 THEN NOW() - INTERVAL '5 hours' END,
       NULL
  FROM (SELECT user_id FROM user_roles WHERE role = 'BUYER' ORDER BY user_id OFFSET 10 LIMIT 10) u
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_SAFE_' || lpad(ROW_NUMBER() OVER (ORDER BY m.merchant_id)::text, 5, '0'),
       m.owner_id,
       'inv_safety_stock_in_vi',
       'IN_APP',
       'SYSTEM',
       'HIGH',
       'Cảnh báo tồn kho dưới mức an toàn',
       'Một số SKU của gian hàng ' || m.name || ' đã xuống dưới mức tồn kho an toàn. Kiểm tra kho để bổ sung hàng.',
       NULL,
       'SENT',
       0,
       0,
       NOW() - make_interval(hours => 3),
       NOW() - make_interval(hours => 3),
       NOW() - make_interval(hours => 3),
       NULL,
       NULL
  FROM (SELECT merchant_id, owner_id, name FROM merchants ORDER BY merchant_id OFFSET 5 LIMIT 10) m
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_SHIPFAIL_' || lpad(ROW_NUMBER() OVER (ORDER BY s.shipment_id)::text, 5, '0'),
       s.user_id,
       'shipping_delivery_failed_in_vi',
       'IN_APP',
       'SHIPPING',
       'HIGH',
       'Giao hàng chưa thành công cho đơn ' || s.order_id,
       'Đơn vị vận chuyển chưa giao được đơn ' || s.order_id || '. Vui lòng kiểm tra số điện thoại và địa chỉ nhận hàng.',
       NULL,
       'SENT',
       0,
       0,
       NOW() - make_interval(hours => 8),
       NOW() - make_interval(hours => 8),
       NOW() - make_interval(hours => 8),
       NULL,
       NULL
  FROM (SELECT shipment_id, order_id, user_id FROM shipments WHERE status IN ('IN_TRANSIT', 'PICKED_UP') ORDER BY shipment_id LIMIT 5) s
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notifications
    (noti_id, user_id, template_id, channel, category, priority, subject, content,
     campaign_id, status, retry_count, version, created_at, updated_at, sent_at, read_at, deleted_at)
SELECT 'NOTI_SHIPRET_' || lpad(ROW_NUMBER() OVER (ORDER BY s.shipment_id)::text, 5, '0'),
       s.user_id,
       'shipping_returned_in_vi',
       'IN_APP',
       'SHIPPING',
       'NORMAL',
       'Đơn hàng ' || s.order_id || ' đã được hoàn về người bán',
       'Đơn hàng ' || s.order_id || ' đã được hoàn về người bán. Khoản thanh toán sẽ được xử lý theo chính sách đổi trả.',
       NULL,
       'READ',
       0,
       0,
       NOW() - make_interval(days => 2),
       NOW() - make_interval(days => 2),
       NOW() - make_interval(days => 2),
       NOW() - INTERVAL '40 hours',
       NULL
  FROM (SELECT shipment_id, order_id, user_id FROM shipments WHERE status = 'CANCELLED' ORDER BY shipment_id LIMIT 5) s
ON CONFLICT (noti_id) DO NOTHING;

INSERT INTO notification_configurations
    (provider_id, channel, provider_type, config, is_active,
     rate_limit_per_minute, configured_by, version, created_at, updated_at)
VALUES
    ('PROV_EMAIL_SENDGRID', 'EMAIL', 'SENDGRID',
     '{"fromEmail":"no-reply@aionn.vn","fromName":"Aionn","sandbox":true}'::jsonb,
     true, 120, (SELECT user_id FROM user_roles WHERE role = 'SYSTEM_ADMIN' ORDER BY user_id LIMIT 1), 0, NOW(), NOW()),
    ('PROV_SMS_TWILIO', 'SMS', 'TWILIO',
     '{"accountSid":"AC_DEMO","fromNumber":"+84900000000","sandbox":true}'::jsonb,
     true, 60, (SELECT user_id FROM user_roles WHERE role = 'SYSTEM_ADMIN' ORDER BY user_id LIMIT 1), 0, NOW(), NOW()),
    ('PROV_PUSH_FCM', 'PUSH', 'FCM',
     '{"projectId":"aionn-demo","serverKey":"demo-key"}'::jsonb,
     true, 300, (SELECT user_id FROM user_roles WHERE role = 'SYSTEM_ADMIN' ORDER BY user_id LIMIT 1), 0, NOW(), NOW()),
    ('PROV_INAPP_WS', 'IN_APP', 'WEBSOCKET',
     '{"endpoint":"/ws/notifications","topic":"/user/queue/notifications"}'::jsonb,
     true, 600, (SELECT user_id FROM user_roles WHERE role = 'SYSTEM_ADMIN' ORDER BY user_id LIMIT 1), 0, NOW(), NOW())
ON CONFLICT (provider_id) DO NOTHING;
