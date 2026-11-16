package com.agnitio.api.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the caller's IP for rate limiting, honoring the first X-Forwarded-For hop
 * set by a reverse proxy.
 */
final class ClientIpResolver {

    private static final String FALLBACK = "unknown";

    private ClientIpResolver() {
    }

    static String resolve(HttpServletRequest request) {
        if (request == null) {
            return FALLBACK;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : FALLBACK;
    }
}
