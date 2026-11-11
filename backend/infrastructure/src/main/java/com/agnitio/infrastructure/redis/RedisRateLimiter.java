package com.agnitio.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class RedisRateLimiter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    public boolean tryAcquire(String actionKey, int maxRequests, Duration window) {
        String key = RATE_LIMIT_PREFIX + actionKey;
        Long current = redisTemplate.opsForValue().increment(key);
        if (current == null) {
            return true;
        }
        if (current == 1) {
            redisTemplate.expire(key, window);
        }
        return current <= maxRequests;
    }
}
