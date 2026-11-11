package com.agnitio.application.command;

import java.util.Objects;
import java.util.UUID;

public record SearchPhotosBySelfieCommand(
        UUID eventId,
        byte[] selfieBytes,
        String fileName,
        boolean consentGiven,
        String clientIp
) {
    public SearchPhotosBySelfieCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(selfieBytes, "selfieBytes must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(clientIp, "clientIp must not be null");
    }
}
