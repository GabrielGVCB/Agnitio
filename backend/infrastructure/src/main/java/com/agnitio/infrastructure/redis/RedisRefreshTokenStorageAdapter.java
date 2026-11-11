package com.agnitio.infrastructure.redis;

import com.agnitio.application.ports.output.RefreshTokenStorage;
import com.agnitio.domain.photographer.PhotographerId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class RedisRefreshTokenStorageAdapter implements RefreshTokenStorage {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String PHOTOGRAPHER_TOKENS_PREFIX = "auth:photographer_tokens:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStorageAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    @Override
    public void store(String refreshToken, PhotographerId photographerId, Duration ttl) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userKey = PHOTOGRAPHER_TOKENS_PREFIX + photographerId.value();

        redisTemplate.opsForValue().set(tokenKey, photographerId.value().toString(), ttl);
        redisTemplate.opsForSet().add(userKey, refreshToken);
        redisTemplate.expire(userKey, ttl);
    }

    @Override
    public Optional<PhotographerId> findPhotographerIdByToken(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String idStr = redisTemplate.opsForValue().get(tokenKey);
        if (idStr == null || idStr.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(PhotographerId.of(idStr));
    }

    @Override
    public void revoke(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String idStr = redisTemplate.opsForValue().get(tokenKey);
        if (idStr != null) {
            String userKey = PHOTOGRAPHER_TOKENS_PREFIX + idStr;
            redisTemplate.opsForSet().remove(userKey, refreshToken);
        }
        redisTemplate.delete(tokenKey);
    }

    @Override
    public void revokeAllForPhotographer(PhotographerId photographerId) {
        String userKey = PHOTOGRAPHER_TOKENS_PREFIX + photographerId.value();
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            }
        }
        redisTemplate.delete(userKey);
    }
}
