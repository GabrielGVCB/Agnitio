package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.ConfirmUploadRequestDto;
import com.agnitio.api.controller.dto.ConfirmUploadResponseDto;
import com.agnitio.api.controller.dto.PhotoGalleryResponseDto;
import com.agnitio.api.controller.dto.RequestUploadUrlsRequestDto;
import com.agnitio.api.controller.dto.UploadUrlsResponseDto;
import com.agnitio.application.command.ConfirmPhotoUploadCommand;
import com.agnitio.application.command.RequestUploadUrlsCommand;
import com.agnitio.application.dto.EventGalleryItem;
import com.agnitio.application.dto.UploadUrlItem;
import com.agnitio.application.usecase.photo.ConfirmPhotoUploadUseCase;
import com.agnitio.application.usecase.photo.ListEventPhotosUseCase;
import com.agnitio.application.usecase.photo.RequestUploadUrlsUseCase;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.PhotographerId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/photos")
public class EventPhotoController {

    private final RequestUploadUrlsUseCase requestUploadUrlsUseCase;
    private final ConfirmPhotoUploadUseCase confirmPhotoUploadUseCase;
    private final ListEventPhotosUseCase listEventPhotosUseCase;

    public EventPhotoController(
            RequestUploadUrlsUseCase requestUploadUrlsUseCase,
            ConfirmPhotoUploadUseCase confirmPhotoUploadUseCase,
            ListEventPhotosUseCase listEventPhotosUseCase
    ) {
        this.requestUploadUrlsUseCase = Objects.requireNonNull(requestUploadUrlsUseCase, "requestUploadUrlsUseCase must not be null");
        this.confirmPhotoUploadUseCase = Objects.requireNonNull(confirmPhotoUploadUseCase, "confirmPhotoUploadUseCase must not be null");
        this.listEventPhotosUseCase = Objects.requireNonNull(listEventPhotosUseCase, "listEventPhotosUseCase must not be null");
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
