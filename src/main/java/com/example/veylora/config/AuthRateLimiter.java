package com.example.veylora.config;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very small in-memory rate limiter for login and register (per key, per minute).
 * Good for a single server. Behind a reverse proxy, getRemoteAddr() is the proxy's
 * address, so use the real client address (forwarded headers) or a proper limiter library.
 */
@Component
public class AuthRateLimiter {

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException() {
            super("Too many attempts");
        }
    }

    private static final long WINDOW_MS = 60_000;
    private static final int PURGE_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    /** Records one attempt for the key, or throws if it already had maxAttempts in the last minute. */
    public void check(String key, int maxAttempts) {
        long now = System.currentTimeMillis();
        if (attempts.size() > PURGE_THRESHOLD) {
            purge(now);
        }
        Deque<Long> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= maxAttempts) {
                throw new RateLimitExceededException();
            }
            times.addLast(now);
        }
    }

    private void purge(long now) {
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || now - last > WINDOW_MS;
            }
        });
    }
}
