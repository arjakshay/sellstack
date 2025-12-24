package com.stack.sellstack.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String DEVICE_ID_KEY = "deviceId";
    private static final String TOKEN_TYPE_KEY = "tokenType";
    private static final String JWT_ID_KEY = "jti";

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValidity) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity * 1000;
        this.refreshTokenValidity = refreshTokenValidity * 1000;
    }

    public String createAccessToken(Authentication authentication, String deviceId) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date validity = new Date(now + this.accessTokenValidity);

        return Jwts.builder()
                .subject(authentication.getName())           // Modern: .subject() instead of .setSubject()
                .claim(AUTHORITIES_KEY, authorities)
                .claim(DEVICE_ID_KEY, deviceId)
                .claim(TOKEN_TYPE_KEY, "ACCESS")
                .issuedAt(new Date(now))                     // Modern: .issuedAt() instead of .setIssuedAt()
                .expiration(validity)                        // Modern: .expiration() instead of .setExpiration()
                .id(UUID.randomUUID().toString())            // Modern: .id() instead of .setId()
                .signWith(secretKey, Jwts.SIG.HS512)         // Modern: Jwts.SIG.HS512 instead of SignatureAlgorithm.HS512
                .compact();
    }

    public String createRefreshToken(Authentication authentication, String deviceId) {
        long now = System.currentTimeMillis();
        Date validity = new Date(now + this.refreshTokenValidity);

        return Jwts.builder()
                .subject(authentication.getName())           // Modern: .subject() instead of .setSubject()
                .claim(DEVICE_ID_KEY, deviceId)
                .claim(TOKEN_TYPE_KEY, "REFRESH")
                .issuedAt(new Date(now))                     // Modern: .issuedAt() instead of .setIssuedAt()
                .expiration(validity)                        // Modern: .expiration() instead of .setExpiration()
                .id(UUID.randomUUID().toString())            // Modern: .id() instead of .setId()
                .signWith(secretKey, Jwts.SIG.HS512)         // Modern: Jwts.SIG.HS512 instead of SignatureAlgorithm.HS512
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .filter(auth -> !auth.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token); // Correct for 0.12.x
            return true;
        } catch (JwtException e) { // Catch the parent exception
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload(); // Changed from getBody() to getPayload()
    }

    public String getDeviceIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get(DEVICE_ID_KEY, String.class);
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get(TOKEN_TYPE_KEY, String.class);
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getSubject(String token) { // Alternative name
        return parseClaims(token).getSubject();
    }

    public long getAccessTokenValidity() {
        return this.accessTokenValidity; // Returns in milliseconds
    }

    // Add this method for seconds (more useful)
    public long getAccessTokenValidityInSeconds() {
        return this.accessTokenValidity / 1000;
    }

    public String getClaimFromToken(String token, String claimName) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(claimName, String.class);
        } catch (Exception e) {
            log.warn("Failed to get claim '{}' from token: {}", claimName, e.getMessage());
            return null;
        }
    }

    // Or a more generic version that can return any type
    public <T> T getClaimFromToken(String token, String claimName, Class<T> clazz) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(claimName, clazz);
        } catch (Exception e) {
            log.warn("Failed to get claim '{}' from token: {}", claimName, e.getMessage());
            return null;
        }
    }
}