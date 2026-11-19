-- Keep demo reservations consistent with stock: reserved units are not available.
WITH reserved AS (
    SELECT sku_id, warehouse_id, SUM(qty)::int AS qty
      FROM stock_reservations
     WHERE reservation_id LIKE 'RSV_DEMO_%'
       AND status = 'RESERVED'
     GROUP BY sku_id, warehouse_id
)
UPDATE inventory_items AS i
   SET available_qty = GREATEST(i.available_qty - r.qty, 0),
       updated_at = NOW()
  FROM reserved r
 WHERE i.sku_id = r.sku_id
   AND i.warehouse_id = r.warehouse_id;

-- Demo compensation tasks should not be picked up immediately by the worker.
UPDATE ordering_compensation_tasks
   SET next_attempt_at = NOW() + INTERVAL '30 days',
       updated_at = NOW()
 WHERE task_id LIKE 'TASK_DEMO_%'
   AND status = 'PENDING';
