package com.agnitio.application.ports.output;

import java.time.Duration;

public interface PhotoStorage {
    String generatePresignedUploadUrl(String storageKey, Duration duration);
    String generatePresignedDownloadUrl(String storageKey, Duration duration);
}
