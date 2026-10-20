-- Backfill payment_method_id for demo orders using payment gateway information
UPDATE orders o
SET payment_method_id = p.gateway
FROM payments p
WHERE o.payment_id = p.payment_id
  AND o.payment_method_id IS NULL;

-- Normalize address_full_name for demo orders to match user's display name
UPDATE orders o
SET address_full_name = u.display_name
FROM users u
WHERE o.user_id = u.user_id
  AND o.address_full_name LIKE 'Customer %';
