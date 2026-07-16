package com.restaurantmanager.security;

import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.entity.GuestSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_RESTAURANT_ID = "restaurantId";
    private static final String CLAIM_TABLE_ID = "tableId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration staffTokenTtl;
    private final Duration guestTokenTtl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.staff-ttl-minutes:480}") long staffTtlMinutes,
            @Value("${app.jwt.guest-ttl-minutes:240}") long guestTtlMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.staffTokenTtl = Duration.ofMinutes(staffTtlMinutes);
        this.guestTokenTtl = Duration.ofMinutes(guestTtlMinutes);
    }

    public IssuedToken generateStaffToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(staffTokenTtl);
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, AuthPrincipal.PrincipalType.STAFF.name())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_RESTAURANT_ID, user.getRestaurant().getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiry);
    }

    public IssuedToken generateGuestToken(GuestSession session) {
        Instant now = Instant.now();
        Instant expiry = now.plus(guestTokenTtl);
        String token = Jwts.builder()
                .subject(session.getId().toString())
                .claim(CLAIM_TYPE, AuthPrincipal.PrincipalType.GUEST.name())
                .claim(CLAIM_ROLE, "GUEST")
                .claim(CLAIM_RESTAURANT_ID, session.getRestaurant().getId().toString())
                .claim(CLAIM_TABLE_ID, session.getTable().getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiry);
    }

    /**
     * Parses and validates the token, returning the resolved principal.
     * Throws JwtException (or a subclass) if the token is invalid, malformed
     * or expired - callers should treat that as "unauthenticated".
     */
    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        AuthPrincipal.PrincipalType type = AuthPrincipal.PrincipalType.valueOf(claims.get(CLAIM_TYPE, String.class));
        UUID id = UUID.fromString(claims.getSubject());
        UUID restaurantId = UUID.fromString(claims.get(CLAIM_RESTAURANT_ID, String.class));
        String role = claims.get(CLAIM_ROLE, String.class);
        String tableIdClaim = claims.get(CLAIM_TABLE_ID, String.class);
        UUID tableId = tableIdClaim != null ? UUID.fromString(tableIdClaim) : null;

        return new AuthPrincipal(id, restaurantId, tableId, type, role);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public static class InvalidTokenException extends JwtException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
