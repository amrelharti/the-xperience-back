package com.thexperience.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final SignatureAlgorithm alg;
    private final long defaultTtlSeconds;

    public JwtService(
            // Preferred: Base64 (property or env var)
            @Value("${app.jwt-secret-base64:${JWT_SECRET_BASE64:}}") String base64Secret,
            // Fallback: raw secret (property or env var)
            @Value("${app.jwt-secret:${JWT_SECRET:}}") String rawSecret,
            @Value("${app.jwt-alg:HS256}") String algName,
            @Value("${app.jwt-ttl-seconds:604800}") long defaultTtlSeconds
    ) {
        byte[] keyBytes;

        if (base64Secret != null && !base64Secret.isBlank()) {
            keyBytes = Decoders.BASE64.decode(base64Secret.trim());
        } else if (rawSecret != null && !rawSecret.isBlank()) {
            keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException(
                    "Missing JWT secret. Set either app.jwt-secret-base64 / JWT_SECRET_BASE64 (preferred) " +
                            "or app.jwt-secret / JWT_SECRET (raw >= 32 bytes).");
        }

        if (keyBytes.length < 32) { // 256 bits
            throw new IllegalArgumentException("JWT HMAC key must be >= 256 bits, provided: " + (keyBytes.length * 8) + " bits");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.alg = SignatureAlgorithm.forName(algName);
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public String issueToken(UUID userId, String role, long ttlSeconds) {
        long ttl = ttlSeconds > 0 ? ttlSeconds : defaultTtlSeconds;
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .addClaims(Map.of("role", role))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttl)))
                .signWith(key, alg)
                .compact();
    }

    public String issueToken(UUID userId, String role) {
        return issueToken(userId, role, -1);
    }

    public Claims parse(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
