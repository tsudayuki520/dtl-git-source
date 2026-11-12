package com.dlust.sportbackend.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisRegistrationGate implements RegistrationGate {
    private static final DefaultRedisScript<Long> ACQUIRE = script("lua/registration-acquire.lua");
    private static final DefaultRedisScript<Long> RELEASE = script("lua/registration-release.lua");
    private final StringRedisTemplate redis;
    private final String prefix;
    private final long leaseMillis;
    private final long waitMillis;

    public RedisRegistrationGate(StringRedisTemplate redis,
            @Value("${sport.registration.redis-key-prefix:sport:registration:}") String prefix,
            @Value("${sport.registration.lease-millis:30000}") long leaseMillis,
            @Value("${sport.registration.wait-millis:2000}") long waitMillis) {
        if (prefix == null || prefix.isBlank() || prefix.contains("{") || prefix.contains("}")
                || leaseMillis <= 0 || waitMillis < 0) {
            throw new IllegalArgumentException("Invalid registration Redis lock configuration");
        }
        this.redis = redis;
        this.prefix = prefix;
        this.leaseMillis = leaseMillis;
        this.waitMillis = waitMillis;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public Lease acquire(List<Long> userIds, Long eventId) {
        List<String> keys = new ArrayList<>();
        userIds.stream().distinct().sorted().forEach(id -> keys.add(prefix + "{registration}:user:" + id));
        keys.add(prefix + "{registration}:event:" + eventId);
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
        do {
            try {
                if (Long.valueOf(1).equals(redis.execute(ACQUIRE, keys, token, Long.toString(leaseMillis)))) {
                    return () -> release(keys, token);
                }
            } catch (RuntimeException e) {
                // A timeout can mean Redis applied the script but its response was lost.
                release(keys, token);
                log.warn("报名 Redis 准入失败: {}", e.getClass().getSimpleName());
                throw new IllegalStateException("报名服务暂不可用，请稍后重试", e);
            }
            if (System.nanoTime() >= deadline) break;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("报名请求已中断，请重试", e);
            }
        } while (true);
        throw new IllegalStateException("报名操作繁忙，请稍后重试");
    }

    private void release(List<String> keys, String token) {
        try {
            redis.execute(RELEASE, keys, token);
        } catch (RuntimeException e) {
            // Do not turn a committed transaction into an apparent failure; TTL cleans up.
            log.warn("报名 Redis 锁释放失败，将由 TTL 回收: {}", e.getClass().getSimpleName());
        }
    }
}
