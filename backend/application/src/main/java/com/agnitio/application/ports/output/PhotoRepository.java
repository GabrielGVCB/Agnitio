package com.agnitio.application.ports.output;

import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository {
    void save(Photo photo);
    void saveAll(List<Photo> photos);
    Optional<Photo> findById(PhotoId id);
    List<Photo> findByEventId(EventId eventId);
    List<Photo> findAllById(List<PhotoId> ids);
}
