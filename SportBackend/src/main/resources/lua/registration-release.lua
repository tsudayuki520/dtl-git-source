-- Never delete a new owner's lease after ours expired.
local released = 0
for i, key in ipairs(KEYS) do
    if redis.call('GET', key) == ARGV[1] then
        released = released + redis.call('DEL', key)
    end
end
return released
