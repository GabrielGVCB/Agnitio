package com.agnitio.api.config;

import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.FaceSearchClient;
import com.agnitio.application.ports.output.PasswordEncoderPort;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.application.ports.output.RateLimiterPort;
import com.agnitio.application.ports.output.RefreshTokenStorage;
import com.agnitio.application.ports.output.TokenService;
import com.agnitio.application.usecase.auth.AuthenticatePhotographerUseCase;
import com.agnitio.application.usecase.auth.LogoutUseCase;
import com.agnitio.application.usecase.auth.RefreshTokenUseCase;
import com.agnitio.application.usecase.auth.RegisterPhotographerUseCase;
import com.agnitio.application.usecase.event.CreateEventUseCase;
import com.agnitio.application.usecase.event.DeleteEventUseCase;
import com.agnitio.application.usecase.event.GetEventByIdUseCase;
import com.agnitio.application.usecase.event.ListPhotographerEventsUseCase;
import com.agnitio.application.usecase.event.SearchPublicEventsUseCase;
import com.agnitio.application.usecase.event.UpdateEventUseCase;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.application.usecase.photo.ConfirmPhotoUploadUseCase;
import com.agnitio.application.usecase.photo.ListEventPhotosUseCase;
import com.agnitio.application.usecase.photo.RegisterUploadedPhotoUseCase;
import com.agnitio.application.usecase.photo.RequestUploadUrlsUseCase;
import com.agnitio.application.usecase.photo.SearchPhotosBySelfieUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class ApplicationConfig {

    @Bean
    public RegisterUploadedPhotoUseCase registerUploadedPhotoUseCase(
            PhotoRepository photoRepository,
            Optional<EventPublisher> eventPublisher
    ) {
        return new RegisterUploadedPhotoUseCase(
                photoRepository,
                eventPublisher.orElse((routingKey, event) -> {})
        );
    }

    @Bean
    public RegisterPhotographerUseCase registerPhotographerUseCase(
            PhotographerRepository photographerRepository,
            PasswordEncoderPort passwordEncoder
    ) {
        return new RegisterPhotographerUseCase(photographerRepository, passwordEncoder);
    }

    @Bean
    public AuthenticatePhotographerUseCase authenticatePhotographerUseCase(
            PhotographerRepository photographerRepository,
            PasswordEncoderPort passwordEncoder,
            TokenService tokenService,
            RefreshTokenStorage refreshTokenStorage
    ) {
        return new AuthenticatePhotographerUseCase(
                photographerRepository,
                passwordEncoder,
                tokenService,
                refreshTokenStorage
        );
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            PhotographerRepository photographerRepository,
            TokenService tokenService,
            RefreshTokenStorage refreshTokenStorage
    ) {
        return new RefreshTokenUseCase(photographerRepository, tokenService, refreshTokenStorage);
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenStorage refreshTokenStorage) {
        return new LogoutUseCase(refreshTokenStorage);
    }

    @Bean
    public CreateEventUseCase createEventUseCase(
            EventRepository eventRepository,
            PhotographerRepository photographerRepository
    ) {
        return new CreateEventUseCase(eventRepository, photographerRepository);
    }

    @Bean
    public GetEventByIdUseCase getEventByIdUseCase(EventRepository eventRepository) {
        return new GetEventByIdUseCase(eventRepository);
    }

    @Bean
    public ListPhotographerEventsUseCase listPhotographerEventsUseCase(EventRepository eventRepository) {
        return new ListPhotographerEventsUseCase(eventRepository);
    }

    @Bean
    public UpdateEventUseCase updateEventUseCase(EventRepository eventRepository) {
        return new UpdateEventUseCase(eventRepository);
    }

    @Bean
    public SearchPublicEventsUseCase searchPublicEventsUseCase(
            EventRepository eventRepository,
            RateLimiterPort rateLimiter
    ) {
        return new SearchPublicEventsUseCase(eventRepository, rateLimiter);
    }

    @Bean
    public DeleteEventUseCase deleteEventUseCase(EventRepository eventRepository) {
        return new DeleteEventUseCase(eventRepository);
    }

    @Bean
    public RequestUploadUrlsUseCase requestUploadUrlsUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage
    ) {
        return new RequestUploadUrlsUseCase(eventRepository, photoRepository, photoStorage);
    }

    @Bean
    public ConfirmPhotoUploadUseCase confirmPhotoUploadUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            Optional<EventPublisher> eventPublisher,
            Optional<PhotoProgressTracker> photoProgressTracker
    ) {
        return new ConfirmPhotoUploadUseCase(
                eventRepository,
                photoRepository,
                eventPublisher.orElse((routingKey, event) -> {}),
                photoProgressTracker.orElse(null)
        );
    }

    @Bean
    public ListEventPhotosUseCase listEventPhotosUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage
    ) {
        return new ListEventPhotosUseCase(eventRepository, photoRepository, photoStorage);
    }

    @Bean
    public SearchPhotosBySelfieUseCase searchPhotosBySelfieUseCase(
            FaceSearchClient faceSearchClient,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage,
            RateLimiterPort rateLimiter
    ) {
        return new SearchPhotosBySelfieUseCase(faceSearchClient, photoRepository, photoStorage, rateLimiter);
    }
}
