package com.eduauth.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * In-memory JWT blacklist used for logout.
 *
 * NOTE: This store is volatile — it is cleared on every server restart.
 *       For production, replace with a Redis-backed store keyed by jti or token hash
 *       with a TTL matching jwt.expiration-ms.
 */
@Service
public class TokenBlacklistService {

    // Thread-safe set; synchronizedSet wraps a HashSet for concurrent access
    private final Set<String> blacklistedTokens =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Add a token to the blacklist (e.g., on logout).
     */
    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Returns true if the token has been explicitly invalidated.
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
