package com.agnitio.api.controller;

import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {

    private final PhotoRepository photoRepository;

    public PhotoController(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhotoResponseDto> getPhotoById(@PathVariable UUID id) {
        return photoRepository.findById(new PhotoId(id))
                .map(this::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private PhotoResponseDto toResponseDto(Photo photo) {
        return new PhotoResponseDto(
                photo.getId().value(),
                photo.getEventId().value(),
                photo.getOriginalFileName(),
                photo.getStatus().name(),
                photo.getCreatedAt()
        );
    }
}
