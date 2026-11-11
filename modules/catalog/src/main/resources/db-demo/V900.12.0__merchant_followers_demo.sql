INSERT INTO merchant_followers (merchant_id, user_id, followed_at)
SELECT m.merchant_id,
       u.user_id,
       NOW() - (u.rn * INTERVAL '1 day')
FROM merchants m
JOIN (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    ORDER BY user_id
    LIMIT 50
) u ON TRUE
ON CONFLICT (merchant_id, user_id) DO NOTHING;
