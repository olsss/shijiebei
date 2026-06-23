package com.worldcup.importreview.service;

import java.nio.file.Path;

public record ImportFileArchiveResult(
        boolean moved,
        Path source,
        Path destination,
        String message
) {
    public static ImportFileArchiveResult moved(Path source, Path destination) {
        return new ImportFileArchiveResult(true, source, destination, "Source JSON archived to " + destination);
    }

    public static ImportFileArchiveResult skipped(Path source, String message) {
        return new ImportFileArchiveResult(false, source, null, message);
    }
}
