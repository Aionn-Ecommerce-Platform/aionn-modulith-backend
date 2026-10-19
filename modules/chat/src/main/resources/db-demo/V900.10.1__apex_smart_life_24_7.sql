UPDATE chat_merchant_auto_replies
SET working_hour_start = '00:00:00',
    working_hour_end = '23:59:59',
    working_days = '["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]'::jsonb,
    updated_at = NOW()
WHERE merchant_id = 'MER_002';
