package com.agnitio.application.ports.output;

import java.time.Duration;

public interface RateLimiterPort {
    boolean tryAcquire(String actionKey, int maxRequests, Duration window);
}
