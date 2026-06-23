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
import java.util.Locale;

@Component
public class JsonArchiveScanner {
    private final ObjectMapper objectMapper;

    public JsonArchiveScanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArchiveScanResult scan(Path archivePath) {
        List<ArchiveScanCandidate> candidates = new ArrayList<>();
        Path normalized = archivePath.toAbsolutePath().normalize();
        addRootJsonFiles(candidates, normalized);
        addDirectory(candidates, normalized, normalized.resolve("analysis"), ImportItemType.ANALYSIS);
        addDirectory(candidates, normalized, normalized.resolve("odds"), ImportItemType.ODDS);
        addDirectory(candidates, normalized, normalized.resolve("sources"), ImportItemType.SOURCE);
        return ArchiveScanResult.from(normalized.toString(), candidates);
    }

    private void addRootJsonFiles(List<ArchiveScanCandidate> candidates, Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var stream = Files.list(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals("_模板.json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        ImportItemType legacyType = path.getFileName().toString().equals("bets.json") ? ImportItemType.BETS : null;
                        addIfExists(candidates, root, path, legacyType);
                    });
        } catch (IOException e) {
            throw new IllegalStateException("无法扫描目录: " + root, e);
        }
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
            ImportItemType resolvedType = resolveType(type, json);
            if (resolvedType == null) {
                candidates.add(new ArchiveScanCandidate(type == null ? ImportItemType.SOURCE : type, relativePath, raw, sha256(raw), file.getFileName().toString(), false, "缺少基础字段: type"));
                return;
            }
            if (type == null && !hasText(json, "type")) {
                candidates.add(new ArchiveScanCandidate(resolvedType, relativePath, raw, sha256(raw), file.getFileName().toString(), false, "缺少基础字段: type"));
                return;
            }
            String summary = extractSummary(resolvedType, file, json);
            String validationFailure = validateBasicFields(resolvedType, json);
            if (validationFailure != null) {
                candidates.add(new ArchiveScanCandidate(resolvedType, relativePath, raw, sha256(raw), summary, false, validationFailure));
                return;
            }
            candidates.add(new ArchiveScanCandidate(resolvedType, relativePath, raw, sha256(raw), summary, true, "ok"));
        } catch (Exception e) {
            ImportItemType fallbackType = type == null ? ImportItemType.SOURCE : type;
            candidates.add(new ArchiveScanCandidate(fallbackType, relativePath, raw, sha256(raw), file.getFileName().toString(), false, "JSON 解析失败: " + e.getMessage()));
        }
    }

    private ImportItemType resolveType(ImportItemType legacyType, JsonNode json) {
        String type = firstText(json, "type", "item_type", "");
        if (type == null || type.isBlank()) {
            return legacyType;
        }
        try {
            return ImportItemType.valueOf(type.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("不支持的 type: " + type);
        }
    }

    private String extractSummary(ImportItemType type, Path file, JsonNode json) {
        JsonNode payload = payload(json);
        return switch (type) {
            case BETS -> "bets.json";
            case TEAM -> firstText(payload, "display_name", "team_key", file.getFileName().toString());
            case PLAYER -> firstText(payload, "display_name", "player_key", file.getFileName().toString());
            case MATCH, MATCH_LINEUP, MATCH_EVENT, MATCH_STATS -> firstText(payload, "match_name", "match", file.getFileName().toString());
            case ANALYSIS -> firstText(payload, "id", "match", firstText(json, "id", "match", file.getFileName().toString()));
            case ODDS -> firstText(payload, "event_id", "match", firstText(json, "event_id", "match", file.getFileName().toString()));
            case SOURCE -> firstText(payload, "id", "match", firstText(json, "id", "match", file.getFileName().toString()));
            case BET -> firstText(payload, "bet_id", "match", file.getFileName().toString());
            case BET_PLAN -> firstText(payload, "title", "plan_key", file.getFileName().toString());
            case POST_REVIEW -> firstText(payload, "title", "review_key", file.getFileName().toString());
            case REVIEW_LESSON -> firstText(payload, "title", "lesson_key", file.getFileName().toString());
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
        JsonNode payload = payload(json);
        return switch (type) {
            case BETS -> json.has("bets") && json.get("bets").isArray()
                    ? null
                    : "缺少基础字段: bets 数组";
            case TEAM -> hasText(payload, "team_key") && hasText(payload, "display_name")
                    ? null
                    : "缺少基础字段: team_key/display_name";
            case PLAYER -> hasText(payload, "player_key") && hasText(payload, "display_name")
                    ? null
                    : "缺少基础字段: player_key/display_name";
            case MATCH -> hasText(payload, "match_key") || hasText(payload, "match_name") || hasText(payload, "match")
                    ? null
                    : "缺少基础字段: match_key 或 match_name";
            case MATCH_LINEUP -> hasText(payload, "match_key") || hasText(payload, "match")
                    ? null
                    : "缺少基础字段: match_key 或 match";
            case MATCH_EVENT -> hasText(payload, "match_key") || hasText(payload, "match")
                    ? null
                    : "缺少基础字段: match_key 或 match";
            case MATCH_STATS -> hasText(payload, "match_key") || hasText(payload, "match")
                    ? null
                    : "缺少基础字段: match_key 或 match";
            case ANALYSIS -> hasText(payload, "id") || hasText(payload, "match") || hasText(json, "id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: id 或 match";
            case ODDS -> hasText(payload, "event_id") || hasText(payload, "match") || hasText(json, "event_id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: event_id 或 match";
            case SOURCE -> hasText(payload, "id") || hasText(payload, "match") || hasText(json, "id") || hasText(json, "match")
                    ? null
                    : "缺少基础字段: id 或 match";
            case BET -> hasText(payload, "bet_id") || hasText(payload, "id")
                    ? null
                    : "缺少基础字段: bet_id 或 id";
            case BET_PLAN -> hasText(payload, "plan_key") || hasText(payload, "id")
                    ? null
                    : "缺少基础字段: plan_key 或 id";
            case POST_REVIEW -> hasText(payload, "review_key") || hasText(payload, "id")
                    ? null
                    : "缺少基础字段: review_key 或 id";
            case REVIEW_LESSON -> hasText(payload, "lesson_text") || hasText(payload, "text")
                    ? null
                    : "缺少基础字段: lesson_text 或 text";
        };
    }

    private JsonNode payload(JsonNode json) {
        JsonNode payload = json.get("payload");
        if (payload != null && payload.isObject()) {
            return payload;
        }
        return json;
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
