-- All keys share the {registration} hash tag. No partial acquisition.
for i, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        return 0
    end
end
for i, key in ipairs(KEYS) do
    redis.call('PSETEX', key, ARGV[2], ARGV[1])
end
return 1
