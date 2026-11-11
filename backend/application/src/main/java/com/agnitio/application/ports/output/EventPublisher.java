package com.agnitio.application.ports.output;

public interface EventPublisher {
    void publish(String routingKey, Object event);
}
