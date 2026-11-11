package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.SearchPhotosBySelfieCommand;
import com.agnitio.application.dto.FaceMatchDto;
import com.agnitio.application.dto.SelfieSearchResultDto;
import com.agnitio.application.ports.output.FaceSearchClient;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.application.ports.output.RateLimiterPort;
import com.agnitio.domain.photo.InvalidBiometricConsentException;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import com.agnitio.domain.photo.RateLimitExceededException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SearchPhotosBySelfieUseCase {

    private static final Duration URL_EXPIRATION = Duration.ofHours(2);
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final double DEFAULT_THRESHOLD = 0.60;
    private static final int DEFAULT_LIMIT = 50;

    private final FaceSearchClient faceSearchClient;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final RateLimiterPort rateLimiter;

    public SearchPhotosBySelfieUseCase(
            FaceSearchClient faceSearchClient,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage,
            RateLimiterPort rateLimiter
    ) {
        this.faceSearchClient = Objects.requireNonNull(faceSearchClient, "faceSearchClient must not be null");
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.photoStorage = Objects.requireNonNull(photoStorage, "photoStorage must not be null");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
    }

    public List<SelfieSearchResultDto> execute(SearchPhotosBySelfieCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        // 1. LGPD: Validate explicit biometric consent
        if (!command.consentGiven()) {
            throw new InvalidBiometricConsentException(
                    "Explicit biometric consent is required to search photos by selfie under LGPD regulations."
            );
        }

        // 2. Abuse Prevention: Rate limiting per client IP
        String rateLimitKey = "selfie:" + command.clientIp();
        if (!rateLimiter.tryAcquire(rateLimitKey, MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("Too many selfie searches. Please wait a minute before trying again.");
        }

        // 3. Query vision service
        List<FaceMatchDto> matches = faceSearchClient.searchFacesBySelfie(
                command.eventId(),
                command.selfieBytes(),
                command.fileName(),
                DEFAULT_THRESHOLD,
                DEFAULT_LIMIT
        );

        // 4. Enrich matched photos with secure pre-signed URLs
        List<SelfieSearchResultDto> results = new ArrayList<>();
        for (FaceMatchDto match : matches) {
            Optional<Photo> photoOpt = photoRepository.findById(new PhotoId(match.photoId()));
            if (photoOpt.isPresent()) {
                Photo photo = photoOpt.get();
                if (photo.getStatus() == PhotoStatus.READY && photo.getPreviewStorageKey() != null) {
                    String previewUrl = photoStorage.generatePresignedDownloadUrl(photo.getPreviewStorageKey(), URL_EXPIRATION);
                    String thumbnailUrl = photo.getThumbnailStorageKey() != null
                            ? photoStorage.generatePresignedDownloadUrl(photo.getThumbnailStorageKey(), URL_EXPIRATION)
                            : previewUrl;

                    results.add(new SelfieSearchResultDto(
                            photo.getId().value(),
                            previewUrl,
                            thumbnailUrl,
                            match.similarity()
                    ));
                }
            }
        }

        return results;
    }
}
