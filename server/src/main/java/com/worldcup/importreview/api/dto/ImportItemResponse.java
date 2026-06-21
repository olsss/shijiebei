package com.worldcup.importreview.api.dto;

public record ImportItemResponse(
        Long id,
        Long jobId,
        String itemType,
        String status,
        String relativePath,
        String sha256,
        String summaryTitle,
        boolean validJson,
        String validationMessage
) {
}
