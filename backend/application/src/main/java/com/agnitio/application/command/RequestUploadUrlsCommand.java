package com.agnitio.application.command;

import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.List;
import java.util.Objects;

public record RequestUploadUrlsCommand(
        EventId eventId,
        PhotographerId photographerId,
        List<FileItem> files
) {
    public RequestUploadUrlsCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(photographerId, "photographerId must not be null");
        Objects.requireNonNull(files, "files must not be null");
        if (files.isEmpty()) {
            throw new IllegalArgumentException("files list cannot be empty");
        }
    }

    public record FileItem(String fileName) {
        public FileItem {
            Objects.requireNonNull(fileName, "fileName must not be null");
            if (fileName.trim().isEmpty()) {
                throw new IllegalArgumentException("fileName cannot be blank");
            }
        }
    }
}
