package com.agnitio.worker.listener;

import com.agnitio.application.command.ProcessPhotoCommand;
import com.agnitio.application.usecase.photo.ProcessPhotoUseCase;
import com.agnitio.domain.photo.PhotoUploadedEvent;
import com.agnitio.infrastructure.messaging.RabbitMqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PhotoUploadedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadedEventListener.class);

    private final ProcessPhotoUseCase processPhotoUseCase;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<Tracer> tracerProvider;
    private final Timer photoProcessingTimer;
    private final Counter successCounter;
    private final Counter failureCounter;

    public PhotoUploadedEventListener(
            ProcessPhotoUseCase processPhotoUseCase,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            ObjectProvider<Tracer> tracerProvider
    ) {
        this.processPhotoUseCase = Objects.requireNonNull(processPhotoUseCase, "processPhotoUseCase must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.tracerProvider = Objects.requireNonNull(tracerProvider, "tracerProvider must not be null");

        this.photoProcessingTimer = Timer.builder("photo.processing.duration")
                .description("Time taken to process and generate photo variants")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.successCounter = Counter.builder("photo.processing.success.total")
                .description("Total number of photos successfully processed")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("photo.processing.failure.total")
                .description("Total number of photo processing failures")
                .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMqConfig.PHOTO_UPLOADED_WORKER_QUEUE)
    public void onPhotoUploaded(
            String messagePayload,
            @Header(name = "traceparent", required = false) String traceparent,
            @Header(name = "X-Correlation-ID", required = false) String headerCorrelationId
    ) {
        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = null;
        Tracer.SpanInScope spanInScope = null;
        long startNanos = System.nanoTime();

        try {
            PhotoUploadedEvent event = objectMapper.readValue(messagePayload, PhotoUploadedEvent.class);
            String correlationId = event.correlationId() != null
                    ? event.correlationId().toString()
                    : (headerCorrelationId != null ? headerCorrelationId : UUID.randomUUID().toString());
            MDC.put("correlationId", correlationId);

            if (tracer != null) {
                span = tracer.nextSpan().name("photo.process").tag("photoId", event.photoId().toString());
                spanInScope = tracer.withSpan(span.start());
            }

            log.info("Received photo.uploaded message: {}", messagePayload);
            ProcessPhotoCommand command = new ProcessPhotoCommand(event.photoId(), event.correlationId());
            processPhotoUseCase.execute(command);

            photoProcessingTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            successCounter.increment();
            log.info("Successfully processed photo: {}", event.photoId());
        } catch (Exception e) {
            failureCounter.increment();
            if (span != null) {
                span.error(e);
            }
            log.error("Failed to process photo from message: {}", messagePayload, e);
            throw new RuntimeException("Error processing photo.uploaded message: " + e.getMessage(), e);
        } finally {
            if (spanInScope != null) {
                spanInScope.close();
            }
            if (span != null) {
                span.end();
            }
            MDC.remove("correlationId");
        }
    }
}
