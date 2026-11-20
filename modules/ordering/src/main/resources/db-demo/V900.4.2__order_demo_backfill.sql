-- Older demo rows do not have a purchase-time list price. Treat the paid
-- price as the original so no false discount is shown for historical orders.
UPDATE order_items
   SET original_unit_price = unit_price
 WHERE original_unit_price IS NULL;

-- Older demo orders were created before purchase-time price snapshots existed.
-- Backfill their original price from the product's current list price when it
-- is higher than the paid price, so the demo still shows realistic discounts.
UPDATE order_items oi
   SET original_unit_price = GREATEST(COALESCE(pv.original_price, pv.price), pv.price)
  FROM product_variants pv
 WHERE pv.sku_id = oi.sku_id
   AND GREATEST(COALESCE(pv.original_price, pv.price), pv.price) > oi.unit_price
   AND (oi.original_unit_price IS NULL OR oi.original_unit_price = oi.unit_price);

UPDATE order_returns
SET reason_type = 'NOT_AS_DESCRIBED'
WHERE reason = 'Sản phẩm không đúng mô tả';

UPDATE order_returns
SET reason_type = 'WRONG_SIZE'
WHERE reason = 'Kích thước không phù hợp';

UPDATE order_returns
SET reason_type = 'DEFECTIVE'
WHERE reason = 'Sản phẩm bị lỗi khi nhận hàng';

UPDATE order_returns
SET reason_type = 'CHANGED_MIND'
WHERE reason = 'Đổi ý, muốn hoàn trả hàng';
