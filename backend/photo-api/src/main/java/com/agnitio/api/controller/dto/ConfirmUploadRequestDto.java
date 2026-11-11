package com.agnitio.api.controller.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ConfirmUploadRequestDto(
        @NotEmpty(message = "photoIds cannot be empty")
        List<UUID> photoIds
) {}
