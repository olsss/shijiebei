package com.worldcup.importreview.service;

import java.util.List;

public record ArchiveScanResult(
        String archivePath,
        int totalItems,
        int validItems,
        int invalidItems,
        List<ArchiveScanCandidate> candidates
) {
    public static ArchiveScanResult from(String archivePath, List<ArchiveScanCandidate> candidates) {
        int valid = (int) candidates.stream().filter(ArchiveScanCandidate::validJson).count();
        return new ArchiveScanResult(archivePath, candidates.size(), valid, candidates.size() - valid, List.copyOf(candidates));
    }
}
