-- Safety-stock alerts are emitted by InventoryEventListener but had no template,
-- so every breach was silently skipped by NotificationDispatchService.
INSERT INTO notification_templates (
    template_id, event_type, channel, category, locale, subject, content,
    placeholders, version, is_active
) VALUES
('inv_safety_stock_in_vi',
 'inventory.safety-stock-breached', 'IN_APP', 'SYSTEM', 'vi-VN',
 'Cảnh báo tồn kho thấp',
 'SKU {{skuId}} tại kho {{warehouseId}} chỉ còn {{availableQty}} sản phẩm (ngưỡng an toàn {{safetyStockQty}}).',
 '["skuId","warehouseId","availableQty","safetyStockQty"]'::jsonb, 1, TRUE),
('inv_safety_stock_in_en',
 'inventory.safety-stock-breached', 'IN_APP', 'SYSTEM', 'en-US',
 'Low stock warning',
 'SKU {{skuId}} in warehouse {{warehouseId}} has only {{availableQty}} units left (safety stock {{safetyStockQty}}).',
 '["skuId","warehouseId","availableQty","safetyStockQty"]'::jsonb, 1, TRUE)
ON CONFLICT (event_type, channel, locale) DO NOTHING;
