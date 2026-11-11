INSERT INTO notification_templates (
    template_id, event_type, channel, category, locale, subject, content,
    placeholders, version, is_active
) VALUES
('merchant_follower_in_vi',
 'catalog.merchant-follower-update', 'IN_APP', 'PROMOTION', 'vi-VN',
 '{{title}}', '{{message}}',
 '["title","message","targetUrl","merchantId"]'::jsonb, 1, TRUE),
('merchant_follower_push_vi',
 'catalog.merchant-follower-update', 'PUSH', 'PROMOTION', 'vi-VN',
 '{{title}}', '{{message}}',
 '["title","message","targetUrl","merchantId"]'::jsonb, 1, TRUE),
('merchant_follower_in_en',
 'catalog.merchant-follower-update', 'IN_APP', 'PROMOTION', 'en-US',
 '{{title}}', '{{message}}',
 '["title","message","targetUrl","merchantId"]'::jsonb, 1, TRUE),
('merchant_follower_push_en',
 'catalog.merchant-follower-update', 'PUSH', 'PROMOTION', 'en-US',
 '{{title}}', '{{message}}',
 '["title","message","targetUrl","merchantId"]'::jsonb, 1, TRUE)
ON CONFLICT (event_type, channel, locale) DO NOTHING;
