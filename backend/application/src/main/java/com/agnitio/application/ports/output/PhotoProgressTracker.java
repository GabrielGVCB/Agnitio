package com.agnitio.application.ports.output;

import com.agnitio.application.dto.EventProgress;

import java.util.UUID;

public interface PhotoProgressTracker {
    void initProgress(UUID eventId, int totalPhotos);

    void incrementProcessed(UUID eventId);

    void incrementFailed(UUID eventId);

    EventProgress getProgress(UUID eventId);
}
