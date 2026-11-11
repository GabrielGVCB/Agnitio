package com.agnitio.api.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    @Test
    @DisplayName("should use existing X-Correlation-ID header if provided and populate MDC")
    @SuppressWarnings("unchecked")
    void shouldUseExistingCorrelationId() throws ServletException, IOException {
        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        CorrelationIdFilter filter = new CorrelationIdFilter(tracerProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "custom-corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcInsideChain = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> mdcInsideChain.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));

        filter.doFilter(request, response, filterChain);

        assertThat(mdcInsideChain.get()).isEqualTo("custom-corr-123");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("custom-corr-123");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("should generate new UUID correlationId if header is missing")
    @SuppressWarnings("unchecked")
    void shouldGenerateCorrelationIdWhenMissing() throws ServletException, IOException {
        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        CorrelationIdFilter filter = new CorrelationIdFilter(tracerProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcInsideChain = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> mdcInsideChain.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));

        filter.doFilter(request, response, filterChain);

        assertThat(mdcInsideChain.get()).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(mdcInsideChain.get());
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("should propagate traceId in response header when active span exists")
    @SuppressWarnings("unchecked")
    void shouldPropagateTraceIdInResponse() throws ServletException, IOException {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);

        CorrelationIdFilter filter = new CorrelationIdFilter(tracerProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.TRACE_ID_HEADER))
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }
}
