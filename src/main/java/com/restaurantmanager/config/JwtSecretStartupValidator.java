package com.restaurantmanager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuses to start the prod profile with the checked-in dev-only JWT secret, or one
 * too short for HS256 - either would let anyone forge staff/guest tokens. Runs as a
 * @PostConstruct so it fires during context refresh, before the embedded server
 * starts accepting connections.
 */
@Component
@Profile("prod")
public class JwtSecretStartupValidator {

    private static final String DEFAULT_SECRET = "dev-only-secret-change-this-to-a-long-random-value-before-deploying";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String secret;

    @PostConstruct
    void validate() {
        if (DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "Refusing to start: JWT_SECRET is still the default dev value. "
                            + "Set a long, random JWT_SECRET environment variable in prod.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Refusing to start: JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long for HS256.");
        }
    }
}
