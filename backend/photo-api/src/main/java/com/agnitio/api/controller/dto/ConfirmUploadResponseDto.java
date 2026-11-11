package com.agnitio.api.controller.dto;

import java.util.List;
import java.util.UUID;

public record ConfirmUploadResponseDto(
        int confirmedCount,
        List<UUID> confirmedPhotoIds
) {}
