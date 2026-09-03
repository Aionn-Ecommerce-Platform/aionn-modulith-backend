UPDATE promotion_campaigns
SET start_date = NOW() - INTERVAL '1 hour',
    end_date = NOW() + INTERVAL '5 years',
    status = 'RUNNING',
    updated_at = NOW(),
    version = version + 1
WHERE campaign_id = 'CAMP_FLASH_DEMO'
  AND type = 'FLASH_SALE';
