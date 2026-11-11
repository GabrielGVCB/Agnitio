package com.agnitio.infrastructure.messaging;

import com.agnitio.application.ports.output.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class RabbitMqEventPublisherAdapter implements EventPublisher {

    private static final String EXCHANGE = "photos";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<Tracer> tracerProvider;

    public RabbitMqEventPublisherAdapter(RabbitTemplate rabbitTemplate, ObjectProvider<Tracer> tracerProvider) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
        this.tracerProvider = Objects.requireNonNull(tracerProvider, "tracerProvider must not be null");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void publish(String routingKey, Object event) {
        try {
            byte[] body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
            Message message = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .build();

            Tracer tracer = tracerProvider.getIfAvailable();
            if (tracer != null && tracer.currentSpan() != null) {
                String traceId = tracer.currentSpan().context().traceId();
                String spanId = tracer.currentSpan().context().spanId();
                if (traceId != null && spanId != null) {
                    String traceparent = "00-" + traceId + "-" + spanId + "-01";
                    message.getMessageProperties().setHeader("traceparent", traceparent);
                    message.getMessageProperties().setHeader("X-Trace-Id", traceId);
                }
            }
            String correlationId = MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
            }

            rabbitTemplate.send(EXCHANGE, routingKey, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON: " + event, e);
        }
    }
}
