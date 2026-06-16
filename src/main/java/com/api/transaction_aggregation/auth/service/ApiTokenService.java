package com.api.transaction_aggregation.auth.service;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;

/**
 * API Token Service that uses RSA keys from a PKCS12 keystore to sign and verify tokens.
 * These tokens are for machine-to-machine / external API calls, separate from session tokens.
 *
 * - Private key signs the token (this server only)
 * - Public key verifies the token (can be distributed to consuming services)
 */
@Service
public class ApiTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long apiTokenExpirationMs;

    public ApiTokenService(
            @Value("${api.token.keystore-path:keystore/api-keystore.p12}") String keystorePath,
            @Value("${api.token.keystore-password:mzr-keystore-pass}") String keystorePassword,
            @Value("${api.token.key-alias:mzr-api-key}") String keyAlias,
            @Value("${api.token.expiration-ms:3600000}") long apiTokenExpirationMs) {

        this.apiTokenExpirationMs = apiTokenExpirationMs;

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            ClassPathResource resource = new ClassPathResource(keystorePath);
            keyStore.load(resource.getInputStream(), keystorePassword.toCharArray());

            this.privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
            Certificate cert = keyStore.getCertificate(keyAlias);
            this.publicKey = cert.getPublicKey();

            log.info("API token keystore loaded successfully. Algorithm: {}", privateKey.getAlgorithm());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load API token keystore: " + e.getMessage(), e);
        }
    }

    /**
     * Generate an API token signed with the RSA private key.
     * Used for machine-to-machine calls and the API Testing Tool.
     */
    public String generateApiToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + apiTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("tokenType", "api")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey)
                .compact();
    }

    /**
     * Validate an API token using the public key.
     */
    public boolean isApiTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract userId from an API token.
     */
    public Long extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    /**
     * Extract username from an API token.
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Extract token type claim.
     */
    public String extractTokenType(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("tokenType", String.class);
    }

    /**
     * Get the public key (for distribution to consuming services).
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
