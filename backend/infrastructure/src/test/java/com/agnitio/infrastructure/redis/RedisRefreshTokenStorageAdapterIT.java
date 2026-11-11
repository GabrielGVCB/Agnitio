package com.agnitio.infrastructure.redis;

import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DataRedisAutoConfiguration.class, RedisRefreshTokenStorageAdapter.class, RedisRateLimiter.class})
@Import({RedisRefreshTokenStorageAdapter.class, RedisRateLimiter.class})
@Testcontainers
class RedisRefreshTokenStorageAdapterIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisRefreshTokenStorageAdapter storageAdapter;

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Test
    void shouldStoreRetrieveAndRevokeRefreshToken() {
        PhotographerId id = PhotographerId.generate();
        String token = "sample-refresh-token-12345";

        storageAdapter.store(token, id, Duration.ofMinutes(5));

        Optional<PhotographerId> retrieved = storageAdapter.findPhotographerIdByToken(token);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(id);

        storageAdapter.revoke(token);
        assertThat(storageAdapter.findPhotographerIdByToken(token)).isEmpty();
    }

    @Test
    void shouldRevokeAllTokensForPhotographer() {
        PhotographerId id = PhotographerId.generate();
        String token1 = "token-1";
        String token2 = "token-2";

        storageAdapter.store(token1, id, Duration.ofMinutes(5));
        storageAdapter.store(token2, id, Duration.ofMinutes(5));

        assertThat(storageAdapter.findPhotographerIdByToken(token1)).isPresent();
        assertThat(storageAdapter.findPhotographerIdByToken(token2)).isPresent();

        storageAdapter.revokeAllForPhotographer(id);

        assertThat(storageAdapter.findPhotographerIdByToken(token1)).isEmpty();
        assertThat(storageAdapter.findPhotographerIdByToken(token2)).isEmpty();
    }

    @Test
    void shouldRateLimitActions() {
        String key = "test-client-ip";
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire(key, 3, Duration.ofSeconds(10))).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(key, 3, Duration.ofSeconds(10))).isFalse();
    }
}
