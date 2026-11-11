package com.agnitio.infrastructure.messaging;

import com.agnitio.application.ports.output.EventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RabbitMqEventPublisherAdapter implements EventPublisher {

    private static final String EXCHANGE = "photos";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
    }

    @Override
    public void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
    }
}
