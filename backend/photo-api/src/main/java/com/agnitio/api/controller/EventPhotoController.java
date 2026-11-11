package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.ConfirmUploadRequestDto;
import com.agnitio.api.controller.dto.ConfirmUploadResponseDto;
import com.agnitio.api.controller.dto.PhotoGalleryResponseDto;
import com.agnitio.api.controller.dto.RequestUploadUrlsRequestDto;
import com.agnitio.api.controller.dto.SearchSelfieResponseDto;
import com.agnitio.api.controller.dto.UploadUrlsResponseDto;
import com.agnitio.application.command.ConfirmPhotoUploadCommand;
import com.agnitio.application.command.RequestUploadUrlsCommand;
import com.agnitio.application.command.SearchPhotosBySelfieCommand;
import com.agnitio.application.dto.EventGalleryItem;
import com.agnitio.application.dto.SelfieSearchResultDto;
import com.agnitio.application.dto.UploadUrlItem;
import com.agnitio.application.usecase.photo.ConfirmPhotoUploadUseCase;
import com.agnitio.application.usecase.photo.ListEventPhotosUseCase;
import com.agnitio.application.usecase.photo.RequestUploadUrlsUseCase;
import com.agnitio.application.usecase.photo.SearchPhotosBySelfieUseCase;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.PhotographerId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/photos")
public class EventPhotoController {

    private final RequestUploadUrlsUseCase requestUploadUrlsUseCase;
    private final ConfirmPhotoUploadUseCase confirmPhotoUploadUseCase;
    private final ListEventPhotosUseCase listEventPhotosUseCase;
    private final SearchPhotosBySelfieUseCase searchPhotosBySelfieUseCase;
    private final Counter uploadConfirmedCounter;
    private final Counter selfieSearchesCounter;

    public EventPhotoController(
            RequestUploadUrlsUseCase requestUploadUrlsUseCase,
            ConfirmPhotoUploadUseCase confirmPhotoUploadUseCase,
            ListEventPhotosUseCase listEventPhotosUseCase,
            SearchPhotosBySelfieUseCase searchPhotosBySelfieUseCase,
            MeterRegistry meterRegistry
    ) {
        this.requestUploadUrlsUseCase = Objects.requireNonNull(requestUploadUrlsUseCase, "requestUploadUrlsUseCase must not be null");
        this.confirmPhotoUploadUseCase = Objects.requireNonNull(confirmPhotoUploadUseCase, "confirmPhotoUploadUseCase must not be null");
        this.listEventPhotosUseCase = Objects.requireNonNull(listEventPhotosUseCase, "listEventPhotosUseCase must not be null");
        this.searchPhotosBySelfieUseCase = Objects.requireNonNull(searchPhotosBySelfieUseCase, "searchPhotosBySelfieUseCase must not be null");
        this.uploadConfirmedCounter = Counter.builder("photo.upload.confirmed.total")
                .description("Total number of photo uploads confirmed")
                .register(meterRegistry);
        this.selfieSearchesCounter = Counter.builder("photo.selfie.searches.total")
                .description("Total number of selfie searches performed")
                .register(meterRegistry);
    }

    @PostMapping("/upload-urls")
    public ResponseEntity<UploadUrlsResponseDto> requestUploadUrls(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal String photographerIdStr,
            @RequestBody @Valid RequestUploadUrlsRequestDto request
    ) {
        UUID photographerUuid = validatePhotographerId(photographerIdStr);
        RequestUploadUrlsCommand command = new RequestUploadUrlsCommand(
                new EventId(eventId),
                new PhotographerId(photographerUuid),
                request.files().stream()
                        .map(f -> new RequestUploadUrlsCommand.FileItem(f.fileName()))
                        .toList()
        );

        List<UploadUrlItem> items = requestUploadUrlsUseCase.execute(command);
        List<UploadUrlsResponseDto.UploadUrlItemResponseDto> responseItems = items.stream()
                .map(item -> new UploadUrlsResponseDto.UploadUrlItemResponseDto(
                        item.photoId().value(),
                        item.fileName(),
                        item.uploadUrl(),
                        item.storageKey()
                ))
                .toList();

        return ResponseEntity.ok(new UploadUrlsResponseDto(responseItems));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmUploadResponseDto> confirmUpload(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal String photographerIdStr,
            @RequestBody @Valid ConfirmUploadRequestDto request
    ) {
        UUID photographerUuid = validatePhotographerId(photographerIdStr);
        List<PhotoId> photoIds = request.photoIds().stream().map(PhotoId::new).toList();

        ConfirmPhotoUploadCommand command = new ConfirmPhotoUploadCommand(
                new EventId(eventId),
                new PhotographerId(photographerUuid),
                photoIds
        );

        List<Photo> confirmedPhotos = confirmPhotoUploadUseCase.execute(command);
        uploadConfirmedCounter.increment(confirmedPhotos.size());
        List<UUID> confirmedUuids = confirmedPhotos.stream()
                .map(photo -> photo.getId().value())
                .toList();

        return ResponseEntity.ok(new ConfirmUploadResponseDto(confirmedUuids.size(), confirmedUuids));
    }

    @GetMapping
    public ResponseEntity<PhotoGalleryResponseDto> getEventGallery(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String accessCode
    ) {
        List<EventGalleryItem> items = listEventPhotosUseCase.execute(new EventId(eventId), accessCode);
        List<PhotoGalleryResponseDto.PhotoGalleryItemDto> photoDtos = items.stream()
                .map(item -> new PhotoGalleryResponseDto.PhotoGalleryItemDto(
                        item.photoId().value(),
                        item.originalFileName(),
                        item.status().name(),
                        item.originalUrl(),
                        item.previewUrl(),
                        item.thumbnailUrl(),
                        item.createdAt()
                ))
                .toList();

        return ResponseEntity.ok(new PhotoGalleryResponseDto(eventId, photoDtos.size(), photoDtos));
    }

    @PostMapping(value = "/search-by-selfie", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SearchSelfieResponseDto> searchBySelfie(
            @PathVariable UUID eventId,
            @RequestParam("selfie") MultipartFile file,
            @RequestParam(value = "consent", defaultValue = "false") boolean consent,
            HttpServletRequest request
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selfie image file is required");
        }

        String clientIp = resolveClientIp(request);
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename();

        SearchPhotosBySelfieCommand command = new SearchPhotosBySelfieCommand(
                eventId,
                bytes,
                filename,
                consent,
                clientIp
        );

        List<SelfieSearchResultDto> results = searchPhotosBySelfieUseCase.execute(command);
        selfieSearchesCounter.increment();

        List<SearchSelfieResponseDto.SearchSelfieMatchItemDto> items = results.stream()
                .map(r -> new SearchSelfieResponseDto.SearchSelfieMatchItemDto(
                        r.photoId(),
                        r.previewUrl(),
                        r.thumbnailUrl(),
                        r.similarity()
                ))
                .toList();

        return ResponseEntity.ok(new SearchSelfieResponseDto(eventId, items.size(), items));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private UUID validatePhotographerId(String photographerIdStr) {
        if (photographerIdStr == null || photographerIdStr.isBlank()) {
            throw new InvalidCredentialsException("Missing photographer principal");
        }
        try {
            return UUID.fromString(photographerIdStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid photographer id format");
        }
    }
}
