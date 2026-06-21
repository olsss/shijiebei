package com.worldcup.importreview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.importreview.domain.ImportItemType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class JsonArchiveScanner {
    private final ObjectMapper objectMapper;

    public JsonArchiveScanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArchiveScanResult scan(Path archivePath) {
        List<ArchiveScanCandidate> candidates = new ArrayList<>();
        Path normalized = archivePath.toAbsolutePath().normalize();
        addIfExists(candidates, normalized, normalized.resolve("bets.json"), ImportItemType.BETS);
        addDirectory(candidates, normalized, normalized.resolve("analysis"), ImportItemType.ANALYSIS);
        addDirectory(candidates, normalized, normalized.resolve("odds"), ImportItemType.ODDS);
        addDirectory(candidates, normalized, normalized.resolve("sources"), ImportItemType.SOURCE);
        return ArchiveScanResult.from(normalized.toString(), candidates);
    }

    private void addDirectory(List<ArchiveScanCandidate> candidates, Path root, Path directory, ImportItemType type) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals("_模板.json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> addIfExists(candidates, root, path, type));
        } catch (IOException e) {
            throw new IllegalStateException("无法扫描目录: " + directory, e);
        }
    }

    private void addIfExists(List<ArchiveScanCandidate> candidates, Path root, Path file, ImportItemType type) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        String raw;
        try {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            raw = "";
            candidates.add(new ArchiveScanCandidate(type, toRelative(root, file), raw, sha256(raw), file.getFileName().toString(), false, "读取失败: " + e.getMessage()));
            return;
        }

        String relativePath = toRelative(root, file);
        try {
            JsonNode json = objectMapper.readTree(raw);
            String summary = extractSummary(type, file, json);
            String validationFailure = validateBasicFields(type, json);
            if (validationFailure != null) {
                candidates.add(new ArchiveScanCandidate(type, relativePath, raw, sha256(raw), summary, false, validationFailure));
                return;
            }
            candidates.add(new ArchiveScanCandidate(type, relativePath, raw, sha256(raw), summary, true, "ok"));
        } catch (Exception e) {
            candidates.add(new ArchiveScanCandidate(type, relativePath, raw, sha256(raw), file.getFileName().toString(), false, "JSON 解析失败: " + e.getMessage()));
        }
    }

    private String extractSummary(ImportItemType type, Path file, JsonNode json) {
        return switch (type) {
            case BETS -> "bets.json";
            case ANALYSIS -> firstText(json, "id", "match", file.getFileName().toString());
            case ODDS -> firstText(json, "event_id", "match", file.getFileName().toString());
            case SOURCE -> firstText(json, "id", "match", file.getFileName().toString());
        };
    }

    private String firstText(JsonNode json, String firstField, String secondField, String fallback) {
        if (json.hasNonNull(firstField) && !json.get(firstField).asText().isBlank()) {
            return json.get(firstField).asText();
        }
        if (json.hasNonNull(secondField) && !json.get(secondField).asText().isBlank()) {
            return json.get(secondField).asText();
        }
        return fallback;
    }

    private String validateBasicFields(ImportItemType type, JsonNode json) {
        return switch (type) {
            case BETS -> json.has("bets") && json.get("bets").isArray()
                    ? null
                    : "缺少基础字段: bets 数组";
            case ANALYSIS -> hasText(json, "id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: id 或 match";
            case ODDS -> hasText(json, "event_id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: event_id 或 match";
            case SOURCE -> hasText(json, "id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: id 或 match";
        };
    }

    private boolean hasText(JsonNode json, String field) {
        return json.hasNonNull(field) && !json.get(field).asText().isBlank();
    }

    private String toRelative(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
