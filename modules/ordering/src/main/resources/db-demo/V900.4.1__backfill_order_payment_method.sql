-- Backfill payment_method_id for demo orders using payment gateway information
UPDATE orders o
SET payment_method_id = p.gateway
FROM payments p
WHERE o.payment_id = p.payment_id
  AND o.payment_method_id IS NULL;
