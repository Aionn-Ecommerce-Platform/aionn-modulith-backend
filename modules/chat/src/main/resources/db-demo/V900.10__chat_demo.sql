INSERT INTO chat_conversations (
    conversation_id, buyer_id, merchant_id, participants,
    last_message_id, last_message_preview, last_message_type,
    last_message_sender_id, last_message_at, is_archived, version,
    created_at, updated_at
) VALUES
('CONV_DEMO_001', '01KV05RTC7NA4KZMMP8KXX7461', 'MER_001', '["01KV05RTC7NA4KZMMP8KXX7461","MER_001"]'::jsonb,
 'MSG_DEMO_004', 'Cảm ơn shop, mình sẽ đặt hàng ngay.', 'TEXT', '01KV05RTC7NA4KZMMP8KXX7461', NOW() - INTERVAL '1 hour', FALSE, 0,
 NOW() - INTERVAL '3 hours', NOW() - INTERVAL '1 hour'),
('CONV_DEMO_002', '01KV05RTC836YVQ3DF9FZFT2A7', 'MER_002', '["01KV05RTC836YVQ3DF9FZFT2A7","MER_002"]'::jsonb,
 'MSG_DEMO_008', 'Shop đã gửi ảnh thực tế, bạn kiểm tra giúp nhé.', 'TEXT', 'MER_002', NOW() - INTERVAL '2 hours', FALSE, 0,
 NOW() - INTERVAL '6 hours', NOW() - INTERVAL '2 hours'),
('CONV_DEMO_003', '01KV05RTC9AYX7XR183X8EQBF0', 'MER_003', '["01KV05RTC9AYX7XR183X8EQBF0","MER_003"]'::jsonb,
 'MSG_DEMO_012', 'Đơn sẽ được bàn giao cho đơn vị vận chuyển trong hôm nay.', 'TEXT', 'MER_003', NOW() - INTERVAL '30 minutes', FALSE, 0,
 NOW() - INTERVAL '1 day', NOW() - INTERVAL '30 minutes');

INSERT INTO chat_messages (
    message_id, conversation_id, sender_id, sender_role, type, body,
    status, delivered_to, read_by, is_recalled, version, sent_at, updated_at
) VALUES
('MSG_DEMO_001', 'CONV_DEMO_001', '01KV05RTC7NA4KZMMP8KXX7461', 'BUYER', 'TEXT', 'Shop ơi, sản phẩm này còn hàng không?',
 'READ', '["MER_001"]'::jsonb, '["MER_001"]'::jsonb, FALSE, 0, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
('MSG_DEMO_002', 'CONV_DEMO_001', 'MER_001', 'MERCHANT', 'TEXT', 'Chào bạn, sản phẩm hiện vẫn còn hàng.',
 'READ', '["01KV05RTC7NA4KZMMP8KXX7461"]'::jsonb, '["01KV05RTC7NA4KZMMP8KXX7461"]'::jsonb, FALSE, 0, NOW() - INTERVAL '2 hours 40 minutes', NOW() - INTERVAL '2 hours 40 minutes'),
('MSG_DEMO_003', 'CONV_DEMO_001', '01KV05RTC7NA4KZMMP8KXX7461', 'BUYER', 'TEXT', 'Thời gian giao đến Hà Nội khoảng bao lâu?',
 'READ', '["MER_001"]'::jsonb, '["MER_001"]'::jsonb, FALSE, 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
('MSG_DEMO_004', 'CONV_DEMO_001', '01KV05RTC7NA4KZMMP8KXX7461', 'BUYER', 'TEXT', 'Cảm ơn shop, mình sẽ đặt hàng ngay.',
 'SENT', '["MER_001"]'::jsonb, '[]'::jsonb, FALSE, 0, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'),

('MSG_DEMO_005', 'CONV_DEMO_002', '01KV05RTC836YVQ3DF9FZFT2A7', 'BUYER', 'TEXT', 'Cho mình xin thêm ảnh thực tế của sản phẩm.',
 'READ', '["MER_002"]'::jsonb, '["MER_002"]'::jsonb, FALSE, 0, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
('MSG_DEMO_006', 'CONV_DEMO_002', 'MER_002', 'MERCHANT', 'TEXT', 'Được bạn nhé, shop đang chuẩn bị ảnh.',
 'READ', '["01KV05RTC836YVQ3DF9FZFT2A7"]'::jsonb, '["01KV05RTC836YVQ3DF9FZFT2A7"]'::jsonb, FALSE, 0, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours'),
('MSG_DEMO_007', 'CONV_DEMO_002', '01KV05RTC836YVQ3DF9FZFT2A7', 'BUYER', 'TEXT', 'Mình cần xem phần màu sắc và kích thước.',
 'READ', '["MER_002"]'::jsonb, '["MER_002"]'::jsonb, FALSE, 0, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours'),
('MSG_DEMO_008', 'CONV_DEMO_002', 'MER_002', 'MERCHANT', 'TEXT', 'Shop đã gửi ảnh thực tế, bạn kiểm tra giúp nhé.',
 'DELIVERED', '["01KV05RTC836YVQ3DF9FZFT2A7"]'::jsonb, '[]'::jsonb, FALSE, 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),

('MSG_DEMO_009', 'CONV_DEMO_003', '01KV05RTC9AYX7XR183X8EQBF0', 'BUYER', 'TEXT', 'Đơn hàng của mình đã được chuẩn bị chưa shop?',
 'READ', '["MER_003"]'::jsonb, '["MER_003"]'::jsonb, FALSE, 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('MSG_DEMO_010', 'CONV_DEMO_003', 'MER_003', 'MERCHANT', 'TEXT', 'Shop đã đóng gói xong và đang chờ bàn giao.',
 'READ', '["01KV05RTC9AYX7XR183X8EQBF0"]'::jsonb, '["01KV05RTC9AYX7XR183X8EQBF0"]'::jsonb, FALSE, 0, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'),
('MSG_DEMO_011', 'CONV_DEMO_003', '01KV05RTC9AYX7XR183X8EQBF0', 'BUYER', 'TEXT', 'Khi nào đơn vị vận chuyển nhận được hàng?',
 'READ', '["MER_003"]'::jsonb, '["MER_003"]'::jsonb, FALSE, 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
('MSG_DEMO_012', 'CONV_DEMO_003', 'MER_003', 'MERCHANT', 'TEXT', 'Đơn sẽ được bàn giao cho đơn vị vận chuyển trong hôm nay.',
 'SENT', '["01KV05RTC9AYX7XR183X8EQBF0"]'::jsonb, '[]'::jsonb, FALSE, 0, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes');

INSERT INTO chat_merchant_auto_replies (
    merchant_id, is_enabled, greeting, away_message,
    working_hour_start, working_hour_end, working_days, timezone,
    version, created_at, updated_at
) VALUES
('MER_001', TRUE, 'Xin chào! Shop MER_001 đã nhận được tin nhắn của bạn.', 'Shop sẽ phản hồi vào giờ làm việc tiếp theo.', '08:00', '18:00', '[1,2,3,4,5,6]'::jsonb, 'Asia/Ho_Chi_Minh', 0, NOW(), NOW()),
('MER_002', TRUE, 'Xin chào! Shop MER_002 có thể hỗ trợ gì cho bạn?', 'Shop hiện ngoài giờ làm việc và sẽ phản hồi sớm.', '09:00', '19:00', '[1,2,3,4,5,6]'::jsonb, 'Asia/Ho_Chi_Minh', 0, NOW(), NOW()),
('MER_003', TRUE, 'Cảm ơn bạn đã liên hệ shop MER_003.', 'Tin nhắn đã được ghi nhận. Shop sẽ phản hồi vào ngày mai.', '08:30', '17:30', '[1,2,3,4,5]'::jsonb, 'Asia/Ho_Chi_Minh', 0, NOW(), NOW());
