package com.agnitio.worker.listener;

import com.agnitio.application.command.ProcessPhotoCommand;
import com.agnitio.application.usecase.photo.ProcessPhotoUseCase;
import com.agnitio.domain.photo.PhotoUploadedEvent;
import com.agnitio.infrastructure.messaging.RabbitMqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PhotoUploadedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadedEventListener.class);

    private final ProcessPhotoUseCase processPhotoUseCase;
    private final ObjectMapper objectMapper;

    public PhotoUploadedEventListener(ProcessPhotoUseCase processPhotoUseCase, ObjectMapper objectMapper) {
        this.processPhotoUseCase = Objects.requireNonNull(processPhotoUseCase, "processPhotoUseCase must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @RabbitListener(queues = RabbitMqConfig.PHOTO_UPLOADED_WORKER_QUEUE)
    public void onPhotoUploaded(String messagePayload) {
        try {
            log.info("Received photo.uploaded message: {}", messagePayload);
            PhotoUploadedEvent event = objectMapper.readValue(messagePayload, PhotoUploadedEvent.class);
            ProcessPhotoCommand command = new ProcessPhotoCommand(event.photoId(), event.correlationId());
            processPhotoUseCase.execute(command);
            log.info("Successfully processed photo: {}", event.photoId());
        } catch (Exception e) {
            log.error("Failed to process photo from message: {}", messagePayload, e);
            throw new RuntimeException("Error processing photo.uploaded message: " + e.getMessage(), e);
        }
    }
}
