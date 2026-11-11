package com.agnitio.infrastructure.security;

import com.agnitio.application.ports.output.TokenService;
import com.agnitio.domain.photographer.Photographer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtTokenService implements TokenService {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L; // 15 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RsaKeyPairHolder keyPairHolder;

    public JwtTokenService(RsaKeyPairHolder keyPairHolder) {
        this.keyPairHolder = Objects.requireNonNull(keyPairHolder, "keyPairHolder must not be null");
    }

    @Override
    public String generateAccessToken(Photographer photographer) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MS);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(photographer.getId().toString())
                    .claim("email", photographer.getEmail())
                    .claim("role", photographer.getRole().name())
                    .claim("name", photographer.getName())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("agnitio-key-1").build(),
                    claimsSet
            );

            signedJWT.sign(new RSASSASigner(keyPairHolder.getPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RS256 JWT access token", e);
        }
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String extractPhotographerId(String accessToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token format", e);
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            if (!signedJWT.verify(new RSASSAVerifier(keyPairHolder.getPublicKey()))) {
                return false;
            }
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
