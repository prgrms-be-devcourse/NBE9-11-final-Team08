local issuedKey = KEYS[1]
local stockKey = KEYS[2]
local userId = ARGV[1]
local totalQuantity = ARGV[2]

if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

if totalQuantity == '' then
    redis.call('SADD', issuedKey, userId)
    return 1
end

local stock = redis.call('GET', stockKey)
if not stock then
    stock = totalQuantity
    redis.call('SET', stockKey, stock)
end

if tonumber(stock) <= 0 then
    return -2
end

redis.call('DECR', stockKey)
redis.call('SADD', issuedKey, userId)
return 1
