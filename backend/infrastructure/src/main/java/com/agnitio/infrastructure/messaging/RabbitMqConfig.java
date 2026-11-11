package com.agnitio.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PHOTOS_EXCHANGE = "photos";

    @Bean
    public TopicExchange photosTopicExchange() {
        return new TopicExchange(PHOTOS_EXCHANGE, true, false);
    }
}
