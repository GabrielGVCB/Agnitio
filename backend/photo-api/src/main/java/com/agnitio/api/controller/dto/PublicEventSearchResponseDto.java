package com.agnitio.api.controller.dto;

import com.agnitio.application.dto.PublicEventPage;

import java.util.List;

public record PublicEventSearchResponseDto(
        List<PublicEventSummaryDto> items,
        int page,
        int size,
        boolean hasNext
) {
    public static PublicEventSearchResponseDto fromDomain(PublicEventPage page) {
        return new PublicEventSearchResponseDto(
                page.events().stream().map(PublicEventSummaryDto::fromDomain).toList(),
                page.page(),
                page.size(),
                page.hasNext()
        );
    }
}
