package com.restaurantmanager.security;

import com.restaurantmanager.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles POST /api/v1/auth/login per client IP so a script can't brute-force
 * passwords by firing unlimited attempts. In-memory is fine here — the app runs
 * as a single instance, and a restart resetting counters is an acceptable trade-off.
 */
@Component
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final class Attempts {
        int count;
        Instant windowStart = Instant.now();
    }

    private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    public void checkAllowed(String clientIp) {
        Attempts attempts = attemptsByIp.computeIfAbsent(clientIp, key -> new Attempts());
        synchronized (attempts) {
            resetIfWindowElapsed(attempts);
            if (attempts.count >= MAX_ATTEMPTS_PER_WINDOW) {
                throw new TooManyRequestsException("Too many login attempts. Please try again in a few minutes.");
            }
        }
    }

    public void recordFailure(String clientIp) {
        Attempts attempts = attemptsByIp.computeIfAbsent(clientIp, key -> new Attempts());
        synchronized (attempts) {
            resetIfWindowElapsed(attempts);
            attempts.count++;
        }
    }

    public void recordSuccess(String clientIp) {
        attemptsByIp.remove(clientIp);
    }

    private void resetIfWindowElapsed(Attempts attempts) {
        if (Instant.now().isAfter(attempts.windowStart.plus(WINDOW))) {
            attempts.windowStart = Instant.now();
            attempts.count = 0;
        }
    }
}
