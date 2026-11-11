package com.agnitio.worker.listener;

import com.agnitio.application.command.ProcessPhotoCommand;
import com.agnitio.application.usecase.photo.ProcessPhotoUseCase;
import com.agnitio.domain.photo.PhotoUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhotoUploadedEventListenerTest {

    private ProcessPhotoUseCase processPhotoUseCase;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private ObjectProvider<Tracer> tracerProvider;
    private PhotoUploadedEventListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        processPhotoUseCase = mock(ProcessPhotoUseCase.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        meterRegistry = new SimpleMeterRegistry();
        tracerProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(null);

        listener = new PhotoUploadedEventListener(
                processPhotoUseCase,
                objectMapper,
                meterRegistry,
                tracerProvider
        );
    }

    @Test
    @DisplayName("should record metrics and execute use case on valid photo.uploaded message")
    void shouldProcessPhotoAndRecordSuccessMetric() throws Exception {
        UUID photoId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PhotoUploadedEvent event = new PhotoUploadedEvent(
                UUID.randomUUID(),
                correlationId,
                Instant.now(),
                1,
                photoId,
                UUID.randomUUID(),
                "events/123/original/photo.jpg",
                "photo.jpg"
        );
        String payload = objectMapper.writeValueAsString(event);

        listener.onPhotoUploaded(payload, null, null);

        verify(processPhotoUseCase).execute(any(ProcessPhotoCommand.class));
        assertThat(meterRegistry.get("photo.processing.success.total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("photo.processing.failure.total").counter().count()).isEqualTo(0.0);
        assertThat(meterRegistry.get("photo.processing.duration").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should increment failure counter and rethrow when use case fails")
    void shouldIncrementFailureCounterOnException() throws Exception {
        UUID photoId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PhotoUploadedEvent event = new PhotoUploadedEvent(
                UUID.randomUUID(),
                correlationId,
                Instant.now(),
                1,
                photoId,
                UUID.randomUUID(),
                "events/123/original/photo.jpg",
                "photo.jpg"
        );
        String payload = objectMapper.writeValueAsString(event);

        doThrow(new RuntimeException("Storage error")).when(processPhotoUseCase).execute(any(ProcessPhotoCommand.class));

        assertThatThrownBy(() -> listener.onPhotoUploaded(payload, null, null))
                .isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.get("photo.processing.failure.total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("photo.processing.success.total").counter().count()).isEqualTo(0.0);
    }
}
