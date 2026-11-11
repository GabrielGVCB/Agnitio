package com.agnitio.infrastructure.persistence.photo;

import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PhotoRepositoryAdapter implements PhotoRepository {

    private final SpringDataPhotoRepository repository;

    public PhotoRepositoryAdapter(SpringDataPhotoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Photo photo) {
        PhotoJpaEntity entity = toJpaEntity(photo);
        repository.save(entity);
    }

    @Override
    public Optional<Photo> findById(PhotoId id) {
        return repository.findById(id.value()).map(this::toDomainEntity);
    }

    private PhotoJpaEntity toJpaEntity(Photo domain) {
        return new PhotoJpaEntity(
                domain.getId().value(),
                domain.getEventId().value(),
                domain.getOriginalFileName(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    private Photo toDomainEntity(PhotoJpaEntity entity) {
        return new Photo(
                new PhotoId(entity.getId()),
                new EventId(entity.getEventId()),
                entity.getOriginalFileName(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}
