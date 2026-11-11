INSERT INTO notification_templates (
    template_id, event_type, channel, category, locale, subject, content,
    placeholders, version, is_active
) VALUES
('product_back_in_stock_in_vi',
 'catalog.product-back-in-stock', 'IN_APP', 'PROMOTION', 'vi-VN',
 'Sản phẩm bạn quan tâm đã có hàng',
 '{{productName}} đã có hàng trở lại. Mua ngay kẻo lỡ!',
 '["productName","skuId","productId","targetUrl"]'::jsonb, 1, TRUE),
('product_back_in_stock_push_vi',
 'catalog.product-back-in-stock', 'PUSH', 'PROMOTION', 'vi-VN',
 'Sản phẩm bạn quan tâm đã có hàng',
 '{{productName}} đã có hàng trở lại. Mua ngay kẻo lỡ!',
 '["productName","skuId","productId","targetUrl"]'::jsonb, 1, TRUE),
('product_back_in_stock_in_en',
 'catalog.product-back-in-stock', 'IN_APP', 'PROMOTION', 'en-US',
 'A product you wanted is back in stock',
 '{{productName}} is available again. Grab it before it sells out!',
 '["productName","skuId","productId","targetUrl"]'::jsonb, 1, TRUE),
('product_back_in_stock_push_en',
 'catalog.product-back-in-stock', 'PUSH', 'PROMOTION', 'en-US',
 'A product you wanted is back in stock',
 '{{productName}} is available again. Grab it before it sells out!',
 '["productName","skuId","productId","targetUrl"]'::jsonb, 1, TRUE)
ON CONFLICT (event_type, channel, locale) DO NOTHING;
