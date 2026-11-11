package com.agnitio.domain.photo;

import com.agnitio.domain.event.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoTest {

    @Test
    @DisplayName("Should successfully follow the happy path lifecycle: PENDING_UPLOAD -> UPLOADED -> PROCESSING -> READY")
    void shouldFollowHappyPathLifecycle() {
        Photo photo = new Photo(PhotoId.generate(), EventId.generate(), "marathon.jpg", Instant.now());
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.PENDING_UPLOAD);

        photo.markAsUploaded();
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.UPLOADED);

        photo.startProcessing();
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.PROCESSING);

        photo.completeProcessing();
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.READY);
    }

    @Test
    @DisplayName("Should allow transition to FAILED from PENDING_UPLOAD, UPLOADED, and PROCESSING")
    void shouldAllowTransitionToFailed() {
        Photo photo1 = new Photo(PhotoId.generate(), EventId.generate(), "pic1.jpg", Instant.now());
        photo1.markAsFailed();
        assertThat(photo1.getStatus()).isEqualTo(PhotoStatus.FAILED);

        Photo photo2 = new Photo(PhotoId.generate(), EventId.generate(), "pic2.jpg", Instant.now());
        photo2.markAsUploaded();
        photo2.markAsFailed();
        assertThat(photo2.getStatus()).isEqualTo(PhotoStatus.FAILED);

        Photo photo3 = new Photo(PhotoId.generate(), EventId.generate(), "pic3.jpg", Instant.now());
        photo3.markAsUploaded();
        photo3.startProcessing();
        photo3.markAsFailed();
        assertThat(photo3.getStatus()).isEqualTo(PhotoStatus.FAILED);
    }

    @Test
    @DisplayName("Should throw InvalidPhotoStateTransitionException for invalid transitions")
    void shouldThrowOnInvalidTransitions() {
        Photo photo = new Photo(PhotoId.generate(), EventId.generate(), "marathon.jpg", Instant.now());

        // Cannot jump directly from PENDING_UPLOAD to PROCESSING
        assertThatThrownBy(photo::startProcessing)
                .isInstanceOf(InvalidPhotoStateTransitionException.class)
                .hasMessageContaining("PENDING_UPLOAD to PROCESSING");

        // Cannot jump directly from PENDING_UPLOAD to READY
        assertThatThrownBy(photo::completeProcessing)
                .isInstanceOf(InvalidPhotoStateTransitionException.class)
                .hasMessageContaining("PENDING_UPLOAD to READY");

        photo.markAsUploaded();
        photo.startProcessing();
        photo.completeProcessing();

        // Cannot mark as failed once READY
        assertThatThrownBy(photo::markAsFailed)
                .isInstanceOf(InvalidPhotoStateTransitionException.class)
                .hasMessageContaining("READY to FAILED");
    }

    @Test
    @DisplayName("Should generate standard storage keys according to specification")
    void shouldGenerateCorrectStorageKeys() {
        PhotoId photoId = PhotoId.generate();
        EventId eventId = EventId.generate();
        Photo photo = new Photo(photoId, eventId, "runner_10k.JPG", Instant.now());

        assertThat(photo.getOriginalStorageKey())
                .isEqualTo("events/" + eventId.value() + "/original/" + photoId.value() + ".jpg");
        assertThat(photo.getPreviewStorageKey())
                .isEqualTo("events/" + eventId.value() + "/preview/" + photoId.value() + ".webp");
        assertThat(photo.getThumbnailStorageKey())
                .isEqualTo("events/" + eventId.value() + "/thumb/" + photoId.value() + ".webp");
    }
}
