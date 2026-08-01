-- Development/test fixtures. Never include classpath:db-demo in production.



-- -----------------------------------------------------------------------------
-- Squashed from V9.2__seed_user_notifications.sql
-- -----------------------------------------------------------------------------
-- Seed some mock notifications for default buyer_001 to show off on the front-end header popover
INSERT INTO notifications (
    noti_id, user_id, template_id, channel, category, priority,
    subject, content, campaign_id, status, retry_count,
    created_at, updated_at, sent_at, read_at
) VALUES
(
    'not_00000000000000000000000001', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'ORDER', 'HIGH',
    'Giao kiện hàng thành công', 'Kiện hàng SPXVN065617513426 của đơn hàng 2606130F61PQFS đã giao thành công đến bạn.',
    NULL, 'SENT', 0, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NULL
),
(
    'not_00000000000000000000000002', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'HIGH',
    'SIÊU SALE BÁCH HÓA GIẢM ĐẾN 50%', '❤️ Áp thêm mã giảm 500K sẵn trong ví 💚 Mã giảm ngành hàng đến 300K, 150K 💛 Cùng mã miễn phí vận chuyển tận nơi 💙 Nhiều tầng giảm giá - Chốt đơn ngay nha!',
    NULL, 'SENT', 0, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NULL
),
(
    'not_00000000000000000000000003', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'MEDIUM',
    'SĂN VOUCHER XTRA ĐẾN 6 TRIỆU NGAY', 'ðŸŒŸ Cùng mã giảm ngành hàng đến 3 Triệu 💟 Thêm mã giảm 666K, 500K tràn ngập ðŸ“º Săn deal giảm 50% tại Shopee Live 💥 0h sale khủng - Mua sạch giỏ hàng!',
    NULL, 'SENT', 0, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '4 hours'
),
(
    'not_00000000000000000000000004', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'MEDIUM',
    'Thời gian sắp hết!', 'ðŸ—“️ Voucher của bạn sẽ hết hạn vào ngày mai. Mua ngay để tiết kiệm hơn!',
    NULL, 'SENT', 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NULL
),
(
    'not_00000000000000000000000005', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'ORDER', 'HIGH',
    'Giao kiện hàng thành công', 'Kiện hàng SPXVN064677750536 của đơn hàng 2606130G5T9EBC đã giao thành công đến bạn.',
    NULL, 'SENT', 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'
);
