local issuedKey = KEYS[1]
local stockKey = KEYS[2]
local userId = ARGV[1]

if redis.call('SREM', issuedKey, userId) == 1 then
    redis.call('INCR', stockKey)
end

return 1
