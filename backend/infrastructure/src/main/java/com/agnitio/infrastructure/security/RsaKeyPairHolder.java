package com.agnitio.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyPairHolder {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyPairHolder.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public RsaKeyPairHolder(
            @Value("${security.jwt.public-key:}") String publicKeyPem,
            @Value("${security.jwt.private-key:}") String privateKeyPem
    ) {
        if (publicKeyPem != null && !publicKeyPem.isBlank() && privateKeyPem != null && !privateKeyPem.isBlank()) {
            this.publicKey = parsePublicKey(publicKeyPem);
            this.privateKey = parsePrivateKey(privateKeyPem);
            log.info("Initialized RSA key pair from configuration");
        } else {
            KeyPair keyPair = generateEphemeralKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            log.info("Initialized ephemeral 2048-bit RSA key pair for local/test environment");
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    private static KeyPair generateEphemeralKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        try {
            String sanitized = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA public key from PEM", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String sanitized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA private key from PEM", e);
        }
    }
}
