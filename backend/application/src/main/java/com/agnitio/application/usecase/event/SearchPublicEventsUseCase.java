package com.agnitio.application.usecase.event;

import com.agnitio.application.dto.PublicEventPage;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.RateLimiterPort;
import com.agnitio.domain.photo.RateLimitExceededException;

import java.time.Duration;
import java.util.Objects;

/**
 * Lets participants (who have no account) find public events by name or city.
 * Private events are never returned: they are only reachable through the link + access code
 * shared by the photographer.
 */
public class SearchPublicEventsUseCase {

    public static final int MAX_PAGE_SIZE = 20;
    public static final int MAX_QUERY_LENGTH = 100;
    static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final EventRepository eventRepository;
    private final RateLimiterPort rateLimiter;

    public SearchPublicEventsUseCase(EventRepository eventRepository, RateLimiterPort rateLimiter) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
    }

    public PublicEventPage execute(String query, int page, int size, String clientKey) {
        Objects.requireNonNull(clientKey, "clientKey must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!rateLimiter.tryAcquire("event-search:" + clientKey, MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("Too many event searches. Please wait a minute before trying again.");
        }
        return eventRepository.searchPublic(normalize(query), page, size);
    }

    private static String normalize(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.length() > MAX_QUERY_LENGTH ? trimmed.substring(0, MAX_QUERY_LENGTH) : trimmed;
    }
}
