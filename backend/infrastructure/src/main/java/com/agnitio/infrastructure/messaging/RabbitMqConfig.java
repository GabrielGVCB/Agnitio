package com.agnitio.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PHOTOS_EXCHANGE = "photos";
    public static final String PHOTO_UPLOADED_ROUTING_KEY = "photo.uploaded";
    public static final String PHOTO_PROCESSED_ROUTING_KEY = "photo.processed";
    public static final String PHOTO_UPLOADED_WORKER_QUEUE = "photos.worker.uploaded";
    public static final String PHOTO_UPLOADED_WORKER_DLQ = "photos.worker.uploaded.dlq";

    @Bean
    public TopicExchange photosTopicExchange() {
        return new TopicExchange(PHOTOS_EXCHANGE, true, false);
    }

    @Bean
    public Queue photoUploadedWorkerQueue() {
        return QueueBuilder.durable(PHOTO_UPLOADED_WORKER_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PHOTO_UPLOADED_WORKER_DLQ)
                .build();
    }

    @Bean
    public Queue photoUploadedWorkerDlq() {
        return QueueBuilder.durable(PHOTO_UPLOADED_WORKER_DLQ).build();
    }

    @Bean
    public Binding photoUploadedWorkerBinding(Queue photoUploadedWorkerQueue, TopicExchange photosTopicExchange) {
        return BindingBuilder.bind(photoUploadedWorkerQueue)
                .to(photosTopicExchange)
                .with(PHOTO_UPLOADED_ROUTING_KEY);
    }
}
