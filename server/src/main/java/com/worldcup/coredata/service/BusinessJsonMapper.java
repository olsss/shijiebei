package com.worldcup.coredata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.coredata.api.dto.CoreDataMappingResponse;
import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class BusinessJsonMapper {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MatchKeyNormalizer matchKeyNormalizer;

    public BusinessJsonMapper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, MatchKeyNormalizer matchKeyNormalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.matchKeyNormalizer = matchKeyNormalizer;
    }

    private record OddsPayload(String bookmaker, JsonNode node) {
    }

    public List<CoreDataMappingResponse> map(ImportItem item, JsonNode json, String actor) {
        if (item.getItemType() == ImportItemType.ANALYSIS) {
            return mapAnalysis(item, json);
        }
        if (item.getItemType() == ImportItemType.ODDS) {
            return mapOdds(item, json);
        }
        if (item.getItemType() == ImportItemType.SOURCE) {
            return mapSource(item, json);
        }
        if (item.getItemType() == ImportItemType.BETS) {
            return mapBets(item, json);
        }
        return List.of();
    }

    private List<CoreDataMappingResponse> mapAnalysis(ImportItem item, JsonNode json) {
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(json, "match", "match_name", "比赛"), item.getSummaryTitle()),
                text(json, "matchday", "date", "比赛日"),
                text(json, "jc_code", "竞彩编号"),
                json
        );
        String analysisId = fallback(text(json, "id", "analysis_id"), "analysis-" + item.getId());
        Long reportId = insertAndReturnId(
                "INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                item.getId(), matchId, analysisId,
                text(json, "conclusion_type", "结论类型"),
                text(json, "confidence", "置信度"),
                nullableJson(json.get("risks")),
                nullableJson(json.get("recommended")),
                nullableJson(json.get("dimensions")),
                text(json, "narrative_md", "正文"),
                toJson(json)
        );
        mappings.add(insertMapping(item.getId(), "ANALYSIS_REPORT", reportId, "IMPORTED", "比赛分析已导入正式库"));

        JsonNode sources = json.get("sources");
        for (JsonNode source : asNodes(sources)) {
            Long evidenceId = insertAndReturnId(
                    "INSERT INTO source_evidence(import_item_id, match_id, source_type, source_name, source_ref, source_url, summary, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                    item.getId(), matchId, "ANALYSIS_SOURCE",
                    fallback(text(source, "name", "title", "source"), "UNKNOWN"),
                    text(source, "id", "ref", "source_ref"),
                    text(source, "url", "source_url"),
                    fallback(text(source, "summary", "note", "title"), toJson(source)),
                    toJson(source)
            );
            mappings.add(insertMapping(item.getId(), "SOURCE_EVIDENCE", evidenceId, "IMPORTED", "分析来源证据已导入正式库"));
        }
        return mappings;
    }

    private List<CoreDataMappingResponse> mapOdds(ImportItem item, JsonNode json) {
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(json, "match", "match_name", "比赛"), item.getSummaryTitle()),
                text(json, "matchday", "date", "date_beijing", "比赛日"),
                text(json, "jc_code", "竞彩编号"),
                json
        );

        List<OddsPayload> oddsPayloads = collectOddsPayloads(json);
        if (oddsPayloads.isEmpty()) {
            oddsPayloads = List.of(new OddsPayload(fallback(text(json, "source", "bookmaker"), "UNKNOWN"), json));
        }
        for (OddsPayload payload : oddsPayloads) {
            JsonNode oddsNode = payload.node();
            Long snapshotId = insertAndReturnId(
                    "INSERT INTO odds_snapshots(import_item_id, match_id, bookmaker, market_type, odds_value, raw_payload) VALUES (?,?,?,?,?,?)",
                    item.getId(), matchId,
                    fallback(payload.bookmaker(), fallback(text(oddsNode, "name", "bookmaker", "company"), fallback(text(json, "source", "bookmaker"), "UNKNOWN"))),
                    fallback(text(oddsNode, "market", "market_type", "type"), "RAW"),
                    decimal(oddsNode, "odds", "value", "price"),
                    toJson(oddsNode)
            );
            mappings.add(insertMapping(item.getId(), "ODDS_SNAPSHOT", snapshotId, "IMPORTED", "赔率快照已导入正式库"));
        }
        return mappings;
    }

    private List<OddsPayload> collectOddsPayloads(JsonNode json) {
        List<OddsPayload> payloads = new ArrayList<>();
        for (JsonNode company : asNodes(json.get("companies"))) {
            String bookmaker = text(company, "name", "bookmaker", "company");
            List<JsonNode> markets = asNodes(company.get("markets"));
            if (markets.isEmpty()) {
                payloads.add(new OddsPayload(bookmaker, company));
            } else {
                for (JsonNode market : markets) {
                    payloads.add(new OddsPayload(bookmaker, market));
                }
            }
        }
        for (JsonNode market : asNodes(json.get("markets"))) {
            payloads.add(new OddsPayload(text(market, "bookmaker", "company", "name"), market));
        }
        for (JsonNode book : asNodes(firstPresent(json, "all_books", "allBooks"))) {
            List<JsonNode> markets = asNodes(book.get("markets"));
            String bookmaker = text(book, "bookmaker", "company", "name");
            if (markets.isEmpty()) {
                payloads.add(new OddsPayload(bookmaker, book));
            } else {
                for (JsonNode market : markets) {
                    payloads.add(new OddsPayload(bookmaker, market));
                }
            }
        }
        return payloads;
    }

    private List<CoreDataMappingResponse> mapSource(ImportItem item, JsonNode json) {
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(json, "match", "match_name", "比赛"), item.getSummaryTitle()),
                text(json, "matchday", "date", "比赛日"),
                text(json, "jc_code", "竞彩编号"),
                json
        );

        List<JsonNode> snapshots = asNodes(json.get("snapshots"));
        if (snapshots.isEmpty()) {
            snapshots = List.of(json);
        }
        for (JsonNode snapshot : snapshots) {
            Long evidenceId = insertAndReturnId(
                    "INSERT INTO source_evidence(import_item_id, match_id, source_type, source_name, source_ref, source_url, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                    item.getId(), matchId,
                    fallback(text(snapshot, "type", "source_type"), "SOURCE_SNAPSHOT"),
                    fallback(text(snapshot, "name", "title", "source"), fallback(text(json, "source", "title"), "UNKNOWN")),
                    text(snapshot, "id", "ref", "source_ref"),
                    text(snapshot, "url", "source_url"),
                    fallback(text(snapshot, "summary", "note", "title"), toJson(snapshot)),
                    decimal(snapshot, "reliability", "score"),
                    toJson(snapshot)
            );
            mappings.add(insertMapping(item.getId(), "SOURCE_EVIDENCE", evidenceId, "IMPORTED", "来源证据已导入正式库"));
        }

        for (JsonNode conflict : asNodes(json.get("conflicts"))) {
            Long conflictId = insertAndReturnId(
                    "INSERT INTO data_conflicts(import_item_id, match_id, conflict_type, entity_key, field_name, current_value, incoming_value, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                    item.getId(), matchId,
                    fallback(text(conflict, "type", "conflict_type"), "RAW_CONFLICT"),
                    text(conflict, "entity", "entity_key"),
                    text(conflict, "field", "field_name"),
                    text(conflict, "current", "current_value"),
                    text(conflict, "incoming", "incoming_value"),
                    toJson(conflict)
            );
            mappings.add(insertMapping(item.getId(), "DATA_CONFLICT", conflictId, "IMPORTED", "数据冲突已导入正式库"));
        }
        importAliases(json);
        return mappings;
    }

    private List<CoreDataMappingResponse> mapBets(ImportItem item, JsonNode json) {
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        List<JsonNode> bets = asNodes(json.get("bets"));
        if (bets.isEmpty()) {
            bets = List.of(json);
        }
        int index = 0;
        for (JsonNode bet : bets) {
            String matchName = fallback(text(bet, "match", "match_name", "比赛"), fallback(text(json, "match", "比赛"), item.getSummaryTitle()));
            String matchday = fallback(text(bet, "matchday", "date", "比赛日"), text(json, "matchday", "date", "比赛日"));
            String jcCode = fallback(text(bet, "jc_code", "竞彩编号"), text(json, "jc_code", "竞彩编号"));
            Long matchId = upsertMatch(matchName, matchday, jcCode, bet);
            String betId = fallback(text(bet, "bet_id", "id", "编号"), "bet-" + item.getId() + "-" + index);
            Long targetId = insertAndReturnId(
                    "INSERT INTO bets(import_item_id, match_id, bet_id, match_name, market_type, selection_text, stake, odds, hit_status, profit_loss, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    item.getId(), matchId, betId, matchName,
                    text(bet, "market", "market_type", "玩法"),
                    text(bet, "selection", "selection_text", "选择", "投注项"),
                    decimal(bet, "stake", "投注额", "本金"),
                    decimal(bet, "odds", "赔率"),
                    fallback(text(bet, "hit_status", "命中", "status"), "PENDING"),
                    decimal(bet, "profit_loss", "盈亏"),
                    toJson(bet)
            );
            mappings.add(insertMapping(item.getId(), "BET", targetId, "IMPORTED", "下注记录已导入正式库"));
            index++;
        }
        return mappings;
    }

    private Long upsertMatch(String matchName, String matchday, String jcCode, JsonNode rawPayload) {
        String safeMatchName = fallback(matchName, "UNKNOWN_MATCH");
        String key = matchKeyNormalizer.normalize(safeMatchName, matchday, jcCode);
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM matches WHERE match_key = ?",
                (rs, rowNum) -> rs.getLong("id"),
                key
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        insertAndReturnId(
                "INSERT INTO matches(match_key, match_name, matchday, jc_code, status, result_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                key,
                safeMatchName,
                parseDate(matchday),
                jcCode,
                "IMPORTED",
                "UNKNOWN",
                toJson(rawPayload)
        );
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key = ?", Long.class, key);
    }

    private CoreDataMappingResponse insertMapping(Long importItemId, String targetType, Long targetId, String status, String message) {
        Long mappingId = insertAndReturnId(
                "INSERT INTO import_item_mappings(import_item_id, target_type, target_id, mapping_status, message) VALUES (?,?,?,?,?)",
                importItemId,
                targetType,
                targetId,
                status,
                message
        );
        return new CoreDataMappingResponse(mappingId, importItemId, targetType, targetId, status, message);
    }

    private void importAliases(JsonNode json) {
        JsonNode aliases = json.get("aliases");
        if (aliases == null || aliases.isNull() || aliases.isMissingNode()) {
            return;
        }
        if (aliases.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = aliases.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                insertAlias(entry.getKey(), textValue(entry.getValue()));
            }
        } else {
            for (JsonNode alias : asNodes(aliases)) {
                String value = textValue(alias);
                insertAlias(value, value);
            }
        }
    }

    private void insertAlias(String code, String alias) {
        String safeCode = fallback(code, alias);
        String safeAlias = fallback(alias, safeCode);
        if (safeCode == null || safeCode.isBlank()) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO data_dictionaries(dict_type, code, display_name, alias, description) VALUES (?,?,?,?,?)",
                    "ALIAS",
                    truncate(safeCode, 160),
                    truncate(safeAlias, 240),
                    truncate(safeAlias, 240),
                    "来源 JSON 自动导入别名"
            );
        } catch (DataAccessException ignored) {
            // 同一别名可能被多个来源重复确认，保持幂等导入主流程不中断。
        }
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("数据库未返回新增记录 ID");
        }
        return key.longValue();
    }

    private List<JsonNode> asNodes(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> values = new ArrayList<>();
            node.forEach(values::add);
            return values;
        }
        return List.of(node);
    }

    private JsonNode firstPresent(JsonNode node, String... fields) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... fields) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            String text = textValue(value);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return toJson(node);
    }

    private String nullableJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return toJson(node);
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("JSON 序列化失败", cause);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() >= 10) {
            normalized = normalized.substring(0, 10);
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        String value = text(node, fields);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException cause) {
            return null;
        }
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
