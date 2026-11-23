UPDATE shipments
SET registered_at = created_at + INTERVAL '10 minutes'
WHERE tracking_code IS NOT NULL
  AND registered_at IS NULL;

UPDATE shipments
SET picked_at = created_at + INTERVAL '6 hours'
WHERE status IN ('PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED')
  AND picked_at IS NULL;

UPDATE shipments
SET in_transit_at = created_at + INTERVAL '12 hours'
WHERE status IN ('IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED')
  AND in_transit_at IS NULL;

UPDATE shipments
SET out_for_delivery_at = created_at + INTERVAL '20 hours'
WHERE status IN ('OUT_FOR_DELIVERY', 'DELIVERED')
  AND out_for_delivery_at IS NULL;

-- Moved from app/V900.11.3__demo_remaining_tables.sql
INSERT INTO processed_carrier_webhooks (event_id, processed_at)
SELECT 'CARRIER_WEBHOOK_DEMO_' || lpad(gs::text, 5, '0'),
       NOW() - make_interval(hours => (gs * 2)::int)
  FROM generate_series(1, 20) AS gs
ON CONFLICT (event_id) DO NOTHING;
