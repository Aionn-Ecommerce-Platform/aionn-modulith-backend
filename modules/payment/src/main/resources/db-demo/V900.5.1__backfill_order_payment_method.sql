-- Re-run the payment method backfill after the payment demo seed (V900.5.0).
-- V900.4.1 is intentionally left unchanged because it may already be applied.
UPDATE orders o
SET payment_method_id = p.gateway
FROM payments p
WHERE o.payment_id = p.payment_id
  AND o.payment_method_id IS NULL;
