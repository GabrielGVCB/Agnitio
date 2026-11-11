package com.agnitio.application.ports.output;

import com.agnitio.domain.photographer.Photographer;

public interface TokenService {
    String generateAccessToken(Photographer photographer);
    String generateRefreshToken();
    String extractPhotographerId(String accessToken);
    boolean validateAccessToken(String accessToken);
}
