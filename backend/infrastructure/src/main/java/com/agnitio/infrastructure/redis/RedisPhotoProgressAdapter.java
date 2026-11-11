package com.agnitio.infrastructure.redis;

import com.agnitio.application.dto.EventProgress;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Component
public class RedisPhotoProgressAdapter implements PhotoProgressTracker {

    private static final String KEY_PREFIX = "progress:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public RedisPhotoProgressAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    @Override
    public void initProgress(UUID eventId, int totalPhotos) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        String totalKey = KEY_PREFIX + eventId + ":total";
        String processedKey = KEY_PREFIX + eventId + ":processed";
        String failedKey = KEY_PREFIX + eventId + ":failed";

        redisTemplate.opsForValue().increment(totalKey, totalPhotos);
        redisTemplate.expire(totalKey, DEFAULT_TTL);

        redisTemplate.opsForValue().setIfAbsent(processedKey, "0", DEFAULT_TTL);
        redisTemplate.opsForValue().setIfAbsent(failedKey, "0", DEFAULT_TTL);
    }

    @Override
    public void incrementProcessed(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        String key = KEY_PREFIX + eventId + ":processed";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    @Override
    public void incrementFailed(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        String key = KEY_PREFIX + eventId + ":failed";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    @Override
    public EventProgress getProgress(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        String totalVal = redisTemplate.opsForValue().get(KEY_PREFIX + eventId + ":total");
        String processedVal = redisTemplate.opsForValue().get(KEY_PREFIX + eventId + ":processed");
        String failedVal = redisTemplate.opsForValue().get(KEY_PREFIX + eventId + ":failed");

        int total = parseOrDefault(totalVal, 0);
        int processed = parseOrDefault(processedVal, 0);
        int failed = parseOrDefault(failedVal, 0);

        return new EventProgress(total, processed, failed);
    }

    private int parseOrDefault(String val, int defaultVal) {
        if (val == null || val.isBlank()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
