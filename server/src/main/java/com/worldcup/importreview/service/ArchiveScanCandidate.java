package com.worldcup.importreview.service;

import com.worldcup.importreview.domain.ImportItemType;

public record ArchiveScanCandidate(
        ImportItemType type,
        String relativePath,
        String rawJson,
        String sha256,
        String summaryTitle,
        boolean validJson,
        String validationMessage
) {
}
