package com.worldcup.importreview.service;

import com.worldcup.importreview.domain.ImportItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Service
public class ImportFileArchiveService {

    public ImportFileArchiveResult archiveImported(ImportItem item) {
        return archive(item, "imported");
    }

    public ImportFileArchiveResult archiveImported(String archivePath, String relativePath) {
        return archive(archivePath, relativePath, "imported");
    }

    public ImportFileArchiveResult archiveRejected(ImportItem item) {
        return archive(item, "rejected");
    }

    public ImportFileArchiveResult archiveRejected(String archivePath, String relativePath) {
        return archive(archivePath, relativePath, "rejected");
    }

    private ImportFileArchiveResult archive(ImportItem item, String bucket) {
        if (item == null || item.getJob() == null || item.getJob().getArchivePath() == null || item.getRelativePath() == null) {
            return ImportFileArchiveResult.skipped(null, "Source JSON not archived: missing scan root or relative path");
        }
        return archive(item.getJob().getArchivePath(), item.getRelativePath(), bucket);
    }

    private ImportFileArchiveResult archive(String archivePath, String relativePath, String bucket) {
        if (archivePath == null || relativePath == null) {
            return ImportFileArchiveResult.skipped(null, "Source JSON not archived: missing scan root or relative path");
        }

        Path root = Path.of(archivePath).toAbsolutePath().normalize();
        Path relative = Path.of(relativePath).normalize();
        if (relative.isAbsolute() || containsParentTraversal(relative)) {
            return ImportFileArchiveResult.skipped(root.resolve(relative).normalize(), "Source JSON not archived: unsafe relative path");
        }

        Path source = root.resolve(relative).normalize();
        if (!source.startsWith(root)) {
            return ImportFileArchiveResult.skipped(source, "未归档源 JSON：源路径越界");
        }
        if (!Files.exists(source)) {
            return ImportFileArchiveResult.skipped(source, "未归档源 JSON：源文件不存在或已移动");
        }
        if (!Files.isRegularFile(source)) {
            return ImportFileArchiveResult.skipped(source, "未归档源 JSON：源路径不是普通文件");
        }

        Path inboxRoot = "pending".equalsIgnoreCase(root.getFileName() == null ? "" : root.getFileName().toString())
                ? root.getParent()
                : root;
        if (inboxRoot == null) {
            inboxRoot = root;
        }
        Path targetDir = inboxRoot.resolve(bucket).resolve(LocalDate.now().toString()).normalize();
        try {
            Files.createDirectories(targetDir);
            Path destination = uniqueDestination(targetDir, source.getFileName().toString());
            Files.move(source, destination);
            return ImportFileArchiveResult.moved(source, destination);
        } catch (IOException | RuntimeException cause) {
            return ImportFileArchiveResult.skipped(source, "Source JSON not archived: " + cause.getMessage());
        }
    }

    private boolean containsParentTraversal(Path relative) {
        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private Path uniqueDestination(Path targetDir, String fileName) throws IOException {
        Path candidate = targetDir.resolve(fileName).normalize();
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        for (int index = 1; index < 10_000; index++) {
            candidate = targetDir.resolve(base + "-" + index + extension).normalize();
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Unable to generate a non-conflicting archive file name");
    }
}
