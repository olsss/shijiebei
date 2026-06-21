package com.worldcup.importreview.api.dto;

public record ImportJobResponse(
        Long id,
        String archivePath,
        String status,
        int totalItems,
        int validItems,
        int invalidItems,
        String message
) {
}
