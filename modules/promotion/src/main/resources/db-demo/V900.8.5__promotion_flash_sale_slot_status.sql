-- Recalculate the demo slot status from each slot's actual time window. The
-- original slot seed marked the whole current day as RUNNING; this keeps only
-- the window that contains NOW() as RUNNING and marks the rest correctly.
WITH slots(campaign_id) AS (VALUES
    ('01M74HC0WB53JNDNF03G2Z7274'),
    ('SLOT_FLASH_SALE_2'),
    ('SLOT_FLASH_SALE_3'),
    ('SLOT_FLASH_SALE_4'),
    ('SLOT_FLASH_SALE_5'),
    ('SLOT_FLASH_SALE_6')
)
UPDATE promotion_campaigns AS c
   SET status = CASE
           WHEN c.start_date <= NOW() AND c.end_date > NOW() THEN 'RUNNING'
           WHEN c.start_date > NOW() THEN 'SCHEDULED'
           ELSE 'ENDED'
       END,
       updated_at = NOW()
  FROM slots s
 WHERE c.campaign_id = s.campaign_id
   AND c.status <> 'CANCELLED';
