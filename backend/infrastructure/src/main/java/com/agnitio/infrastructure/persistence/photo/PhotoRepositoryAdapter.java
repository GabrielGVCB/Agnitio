package com.agnitio.infrastructure.persistence.photo;

import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public void saveAll(List<Photo> photos) {
        List<PhotoJpaEntity> entities = photos.stream()
                .map(this::toJpaEntity)
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public Optional<Photo> findById(PhotoId id) {
        return repository.findById(id.value()).map(this::toDomainEntity);
    }

    @Override
    public List<Photo> findByEventId(EventId eventId) {
        return repository.findByEventId(eventId.value()).stream()
                .map(this::toDomainEntity)
                .toList();
    }

    @Override
    public List<Photo> findAllById(List<PhotoId> ids) {
        List<UUID> uuidList = ids.stream().map(PhotoId::value).toList();
        return repository.findAllById(uuidList).stream()
                .map(this::toDomainEntity)
                .toList();
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
