package com.agnitio.application.ports.output;

import com.agnitio.domain.photographer.PhotographerId;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStorage {
    void store(String refreshToken, PhotographerId photographerId, Duration ttl);
    Optional<PhotographerId> findPhotographerIdByToken(String refreshToken);
    void revoke(String refreshToken);
    void revokeAllForPhotographer(PhotographerId photographerId);
}
