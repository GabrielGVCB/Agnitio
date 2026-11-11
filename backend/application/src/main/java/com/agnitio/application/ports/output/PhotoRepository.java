package com.agnitio.application.ports.output;

import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;

import java.util.Optional;

public interface PhotoRepository {
    void save(Photo photo);
    Optional<Photo> findById(PhotoId id);
}
