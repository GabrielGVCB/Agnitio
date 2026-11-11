package com.agnitio.worker.config;

import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.ImageProcessor;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.application.usecase.photo.ProcessPhotoUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class WorkerApplicationConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public ProcessPhotoUseCase processPhotoUseCase(
            PhotoRepository photoRepository,
            PhotoStorage photoStorage,
            ImageProcessor imageProcessor,
            Optional<EventPublisher> eventPublisher,
            Optional<PhotoProgressTracker> photoProgressTracker
    ) {
        return new ProcessPhotoUseCase(
                photoRepository,
                photoStorage,
                imageProcessor,
                eventPublisher.orElse((routingKey, event) -> {}),
                photoProgressTracker.orElse(null)
        );
    }
}
