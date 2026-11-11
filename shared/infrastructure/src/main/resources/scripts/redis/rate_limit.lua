-- KEYS[1]: The redis key (e.g., security:ip-rate-limit:127.0.0.1)
-- ARGV[1]: Window size in seconds (TTL)
-- ARGV[2]: Max requests allowed in that window

local current = redis.call('INCR', KEYS[1])

-- if it's the first request in this window, set the expiration
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end

-- check if the current count exceeds the maximum allowed
if current > tonumber(ARGV[2]) then
    return 0 --denied
end

return 1 --allowed
