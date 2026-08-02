INSERT INTO notification_subscriptions (user_id, settings, version, created_at, updated_at) VALUES
('01KV05RTC7NA4KZMMP8KXX7461', '{"PROMOTION:EMAIL":true,"PROMOTION:IN_APP":true,"PROMOTION:PUSH":true,"CHAT:IN_APP":true}'::jsonb, 0, NOW(), NOW()),
('01KV05RTC836YVQ3DF9FZFT2A7', '{"PROMOTION:EMAIL":false,"PROMOTION:IN_APP":true,"PROMOTION:PUSH":true,"CHAT:IN_APP":true}'::jsonb, 0, NOW(), NOW()),
('01KV05RTC9AYX7XR183X8EQBF0', '{"PROMOTION:EMAIL":true,"PROMOTION:IN_APP":true,"PROMOTION:PUSH":false,"CHAT:IN_APP":true}'::jsonb, 0, NOW(), NOW()),
('01KV05RTCATYB3M97J3TJ1NEAP', '{"PROMOTION:EMAIL":true,"PROMOTION:IN_APP":true,"PROMOTION:PUSH":true,"CHAT:IN_APP":true}'::jsonb, 0, NOW(), NOW()),
('01KV05RTCBBPV0GBS7DFTEHY1T', '{"PROMOTION:EMAIL":false,"PROMOTION:IN_APP":true,"PROMOTION:PUSH":false,"CHAT:IN_APP":true}'::jsonb, 0, NOW(), NOW());

INSERT INTO notifications (
    noti_id, user_id, template_id, channel, category, priority,
    subject, content, campaign_id, status, retry_count,
    created_at, updated_at, sent_at, read_at
) VALUES
('NOTI_DEMO_001', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'SHIPPING', 'HIGH',
 'Đơn hàng đã được hủy', 'Đơn hàng ORD_0010 đã được hủy theo yêu cầu. Nếu đã thanh toán, khoản hoàn tiền sẽ được xử lý theo chính sách.',
 NULL, 'SENT', 0, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NULL),
('NOTI_DEMO_002', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'LOW',
 'Voucher mới dành cho bạn', 'Voucher FLASH50K đã có trong chương trình ưu đãi. Hãy kiểm tra điều kiện trước khi sử dụng.',
 'CAMP_FLASH_DEMO', 'READ', 0, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '3 hours'),

('NOTI_DEMO_003', '01KV05RTC836YVQ3DF9FZFT2A7', NULL, 'IN_APP', 'TRANSACTION', 'HIGH',
 'Đang chờ xác nhận thanh toán', 'Đơn hàng ORD_0489 đang chờ hệ thống xác nhận thanh toán.',
 NULL, 'READ', 0, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours'),
('NOTI_DEMO_004', '01KV05RTC836YVQ3DF9FZFT2A7', NULL, 'IN_APP', 'CHAT', 'NORMAL',
 'Bạn có tin nhắn mới', 'Nhà bán hàng MER_002 vừa phản hồi cuộc trò chuyện của bạn.',
 NULL, 'SENT', 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NULL),

('NOTI_DEMO_005', '01KV05RTC9AYX7XR183X8EQBF0', NULL, 'IN_APP', 'SECURITY', 'CRITICAL',
 'Đăng nhập từ thiết bị mới', 'Tài khoản của bạn vừa đăng nhập từ một thiết bị mới. Hãy kiểm tra nếu đây không phải bạn.',
 NULL, 'SENT', 0, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', NULL),
('NOTI_DEMO_006', '01KV05RTC9AYX7XR183X8EQBF0', NULL, 'IN_APP', 'SHIPPING', 'HIGH',
 'Đơn hàng đang được chuẩn bị', 'Nhà bán hàng đang chuẩn bị sản phẩm cho đơn hàng ORD_0229.',
 NULL, 'READ', 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '1 day', NOW() - INTERVAL '20 hours'),

('NOTI_DEMO_007', '01KV05RTCATYB3M97J3TJ1NEAP', NULL, 'IN_APP', 'PROMOTION', 'LOW',
 'Ưu đãi cuối tuần', 'Nhiều sản phẩm đang giảm giá trong chương trình cuối tuần của Aionn.',
 'CAMP_MEGA', 'SENT', 0, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NULL),
('NOTI_DEMO_008', '01KV05RTCATYB3M97J3TJ1NEAP', NULL, 'IN_APP', 'SYSTEM', 'NORMAL',
 'Hồ sơ đã được cập nhật', 'Thông tin hồ sơ của bạn đã được cập nhật thành công.',
 NULL, 'READ', 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

('NOTI_DEMO_009', '01KV05RTCBBPV0GBS7DFTEHY1T', NULL, 'IN_APP', 'TRANSACTION', 'HIGH',
 'Đơn hàng đang được chuẩn bị', 'Nhà bán hàng đang đóng gói sản phẩm cho đơn hàng ORD_0186.',
 NULL, 'SENT', 0, NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '45 minutes', NULL),
('NOTI_DEMO_010', '01KV05RTCBBPV0GBS7DFTEHY1T', NULL, 'IN_APP', 'SYSTEM', 'NORMAL',
 'Thông tin tài khoản', 'Bạn có thể cập nhật địa chỉ nhận hàng và tùy chọn thông báo trong phần hồ sơ.',
 NULL, 'SENT', 0, NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '15 minutes', NULL);
