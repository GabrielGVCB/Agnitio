package com.agnitio.application.usecase.event;

import com.agnitio.application.dto.PublicEventPage;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.RateLimiterPort;
import com.agnitio.domain.photo.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchPublicEventsUseCaseTest {

    private static final String CLIENT = "203.0.113.7";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RateLimiterPort rateLimiter;

    private SearchPublicEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchPublicEventsUseCase(eventRepository, rateLimiter);
    }

    @Test
    void shouldTrimQueryAndDelegateToRepository() {
        PublicEventPage page = new PublicEventPage(List.of(), 0, 12, false);
        when(rateLimiter.tryAcquire(eq("event-search:" + CLIENT), anyInt(), eq(Duration.ofMinutes(1)))).thenReturn(true);
        when(eventRepository.searchPublic("maratona", 0, 12)).thenReturn(page);

        PublicEventPage result = useCase.execute("  maratona  ", 0, 12, CLIENT);

        assertThat(result).isSameAs(page);
    }

    @Test
    void shouldTreatBlankQueryAsListAll() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), eq(Duration.ofMinutes(1)))).thenReturn(true);
        when(eventRepository.searchPublic(null, 1, 5)).thenReturn(new PublicEventPage(List.of(), 1, 5, false));

        useCase.execute("   ", 1, 5, CLIENT);

        verify(eventRepository).searchPublic(null, 1, 5);
    }

    @Test
    void shouldTruncateOverlyLongQuery() {
        String longQuery = "a".repeat(SearchPublicEventsUseCase.MAX_QUERY_LENGTH + 50);
        String expected = "a".repeat(SearchPublicEventsUseCase.MAX_QUERY_LENGTH);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), eq(Duration.ofMinutes(1)))).thenReturn(true);
        when(eventRepository.searchPublic(expected, 0, 12)).thenReturn(new PublicEventPage(List.of(), 0, 12, false));

        useCase.execute(longQuery, 0, 12, CLIENT);

        verify(eventRepository).searchPublic(expected, 0, 12);
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> useCase.execute("x", -1, 12, CLIENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute("x", 0, 0, CLIENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute("x", 0, SearchPublicEventsUseCase.MAX_PAGE_SIZE + 1, CLIENT))
                .isInstanceOf(IllegalArgumentException.class);

        verify(eventRepository, never()).searchPublic(anyString(), anyInt(), anyInt());
    }

    @Test
    void shouldThrowWhenRateLimitExceeded() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), eq(Duration.ofMinutes(1)))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("maratona", 0, 12, CLIENT))
                .isInstanceOf(RateLimitExceededException.class);

        verify(eventRepository, never()).searchPublic(anyString(), anyInt(), anyInt());
    }
}
