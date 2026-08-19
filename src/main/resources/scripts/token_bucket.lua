-- KEYS[1] = bucket key (e.g. "ratelimit:shorten:192.168.1.1")
-- ARGV[1] = maximum bucket capacity
-- ARGV[2] = refill rate (tokens per second)
-- ARGV[3] = current timestamp in seconds

local bucket = redis.call("HMGET", KEYS[1], "tokens", "timestamp")
local tokens = tonumber(bucket[1]) or tonumber(ARGV[1])
local timestamp = tonumber(bucket[2]) or tonumber(ARGV[3])

local elapsed = tonumber(ARGV[3]) - timestamp
local refill = elapsed * tonumber(ARGV[2])
tokens = math.min(tonumber(ARGV[1]), tokens + refill)

if tokens < 1 then
    redis.call("HSET", KEYS[1], "tokens", tokens, "timestamp", ARGV[3])
    redis.call("EXPIRE", KEYS[1], 3600)
    return 0 -- Rejected (rate limit exceeded)
else
    tokens = tokens - 1
    redis.call("HSET", KEYS[1], "tokens", tokens, "timestamp", ARGV[3])
    redis.call("EXPIRE", KEYS[1], 3600)
    return 1 -- Allowed (token consumed)
end
