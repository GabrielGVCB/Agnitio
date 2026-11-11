package com.agnitio.infrastructure.messaging;

import com.agnitio.application.ports.output.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class RabbitMqEventPublisherAdapter implements EventPublisher {

    private static final String EXCHANGE = "photos";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMqEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
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
            rabbitTemplate.send(EXCHANGE, routingKey, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON: " + event, e);
        }
    }
}
