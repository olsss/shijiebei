package com.worldcup.importreview.api.dto;

public record ImportItemDetailResponse(
        ImportItemResponse item,
        String rawJson,
        String rejectionReason
) {
}
