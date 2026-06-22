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
import java.time.LocalDateTime;
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

    private record SelectionPayload(
            String code,
            String name,
            BigDecimal oddsValue,
            BigDecimal impliedProbability,
            String status,
            JsonNode node
    ) {
    }

    private record FactorPayload(String defaultCategory, JsonNode node) {
    }

    private record RiskPayload(Long factorId, JsonNode node) {
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
            String bookmaker = fallback(payload.bookmaker(), fallback(text(oddsNode, "name", "bookmaker", "company"), fallback(text(json, "source", "bookmaker"), "UNKNOWN")));
            String marketCode = marketCode(oddsNode);
            Long snapshotId = insertAndReturnId(
                    "INSERT INTO odds_snapshots(import_item_id, match_id, bookmaker, market_type, odds_value, raw_payload) VALUES (?,?,?,?,?,?)",
                    item.getId(), matchId,
                    bookmaker,
                    marketCode,
                    decimal(oddsNode, "odds", "value", "price", "赔率"),
                    toJson(oddsNode)
            );
            Long marketId = insertAndReturnId(
                    "INSERT INTO odds_market_snapshots(import_item_id, odds_snapshot_id, match_id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    item.getId(), snapshotId, matchId,
                    bookmaker,
                    marketCode,
                    marketName(oddsNode, marketCode),
                    snapshotType(oddsNode),
                    decimal(oddsNode, "handicap", "handicap_line", "line", "盘口", "让球"),
                    text(oddsNode, "line_value", "line", "handicap", "handicap_line", "盘口", "让球"),
                    parseDateTime(text(oddsNode, "captured_at", "capturedAt", "time", "timestamp", "抓取时间")),
                    text(oddsNode, "source_ref", "ref", "id"),
                    toJson(oddsNode)
            );
            for (SelectionPayload selection : collectSelectionPayloads(oddsNode)) {
                insertAndReturnId(
                        "INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                        marketId,
                        truncate(fallback(selection.code(), "RAW"), 160),
                        truncate(fallback(selection.name(), fallback(selection.code(), "原始赔率")), 240),
                        selection.oddsValue(),
                        selection.impliedProbability(),
                        truncate(fallback(selection.status(), "UNKNOWN"), 80),
                        toJson(selection.node())
                );
            }
            mappings.add(insertMapping(item.getId(), "ODDS_SNAPSHOT", snapshotId, "IMPORTED", "赔率快照已导入正式库"));
        }
        return mappings;
    }

    private List<SelectionPayload> collectSelectionPayloads(JsonNode marketNode) {
        List<SelectionPayload> selections = new ArrayList<>();
        JsonNode explicitSelections = firstPresent(marketNode, "selections", "outcomes", "options");
        int index = 0;
        for (JsonNode node : asNodes(explicitSelections)) {
            String code = fallback(text(node, "code", "key", "selection", "name", "label", "outcome", "投注项"), "SELECTION_" + index);
            String name = fallback(text(node, "name", "label", "selection", "outcome", "code", "key", "投注项"), code);
            selections.add(new SelectionPayload(
                    code,
                    name,
                    decimal(node, "odds", "price", "value", "赔率"),
                    decimal(node, "implied_probability", "probability", "prob"),
                    fallback(text(node, "status", "selection_status"), "UNKNOWN"),
                    node
            ));
            index++;
        }
        if (!selections.isEmpty()) {
            return selections;
        }

        JsonNode objectOdds = firstPresent(marketNode, "odds", "prices");
        if (objectOdds != null && objectOdds.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectOdds.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode valueNode = entry.getValue();
                if (valueNode.isObject()) {
                    String code = fallback(text(valueNode, "code", "key", "selection", "label", "outcome", "投注项"), entry.getKey());
                    String name = fallback(text(valueNode, "name", "label", "selection", "outcome", "投注项"), code);
                    selections.add(new SelectionPayload(
                            code,
                            name,
                            decimal(valueNode, "odds", "price", "value", "赔率"),
                            decimal(valueNode, "implied_probability", "probability", "prob"),
                            fallback(text(valueNode, "status", "selection_status"), "UNKNOWN"),
                            valueNode
                    ));
                    continue;
                }
                selections.add(new SelectionPayload(
                        entry.getKey(),
                        entry.getKey(),
                        decimalValue(valueNode),
                        null,
                        "UNKNOWN",
                        valueNode
                ));
            }
            return selections;
        }
        if (objectOdds != null && objectOdds.isArray()) {
            for (JsonNode node : asNodes(objectOdds)) {
                String code = fallback(text(node, "code", "key", "selection", "name", "label", "outcome", "投注项"), "SELECTION_" + index);
                String name = fallback(text(node, "name", "label", "selection", "outcome", "code", "key", "投注项"), code);
                selections.add(new SelectionPayload(
                        code,
                        name,
                        decimal(node, "odds", "price", "value", "赔率"),
                        decimal(node, "implied_probability", "probability", "prob"),
                        fallback(text(node, "status", "selection_status"), "UNKNOWN"),
                        node
                ));
                index++;
            }
            return selections;
        }

        BigDecimal singleOdds = decimal(marketNode, "odds", "value", "price", "赔率");
        if (singleOdds != null) {
            selections.add(new SelectionPayload("RAW", "原始赔率", singleOdds, null, "UNKNOWN", marketNode));
        }
        return selections;
    }

    private String marketCode(JsonNode node) {
        String code = fallback(text(node, "market", "market_type", "type", "key", "code", "玩法"), "RAW");
        return code.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String marketName(JsonNode node, String marketCode) {
        return fallback(text(node, "market_name", "name", "display_name", "玩法名称", "玩法"), marketCode);
    }

    private String snapshotType(JsonNode node) {
        String type = fallback(text(node, "snapshot_type", "phase", "snapshot", "快照类型"), "RAW");
        return type.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
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

        for (FactorPayload factor : collectFactorPayloads(json)) {
            Long factorId = insertContextFactor(item, matchId, factor);
            mappings.add(insertMapping(item.getId(), "MATCH_CONTEXT_FACTOR", factorId, "IMPORTED", "舆情与外部因素已导入正式库"));
            for (JsonNode risk : asNodes(factor.node().get("risks"))) {
                Long riskId = insertRiskAssessment(item, matchId, new RiskPayload(factorId, risk));
                mappings.add(insertMapping(item.getId(), "SENTIMENT_RISK_ASSESSMENT", riskId, "IMPORTED", "舆情风险评分已导入正式库"));
            }
        }
        for (RiskPayload risk : collectTopLevelRiskPayloads(json)) {
            Long riskId = insertRiskAssessment(item, matchId, risk);
            mappings.add(insertMapping(item.getId(), "SENTIMENT_RISK_ASSESSMENT", riskId, "IMPORTED", "舆情风险评分已导入正式库"));
        }
        importAliases(json);
        return mappings;
    }

    private List<FactorPayload> collectFactorPayloads(JsonNode json) {
        List<FactorPayload> factors = new ArrayList<>();
        addFactorArray(factors, json, "external_factors", "OTHER");
        addFactorArray(factors, json, "factors", "OTHER");
        addFactorArray(factors, json, "sentiment_records", "PUBLIC_SENTIMENT");
        addFactorArray(factors, json, "sentiments", "PUBLIC_SENTIMENT");
        addSingleFactor(factors, json, "weather", "WEATHER");
        addSingleFactor(factors, json, "venue", "VENUE");
        addSingleFactor(factors, json, "referee", "REFEREE");
        addSingleFactor(factors, json, "motivation", "MOTIVATION");
        addSingleFactor(factors, json, "public_sentiment", "PUBLIC_SENTIMENT");
        return factors;
    }

    private void addFactorArray(List<FactorPayload> factors, JsonNode json, String field, String defaultCategory) {
        for (JsonNode node : asNodes(json.get(field))) {
            factors.add(new FactorPayload(defaultCategory, node));
        }
    }

    private void addSingleFactor(List<FactorPayload> factors, JsonNode json, String field, String category) {
        JsonNode node = json.get(field);
        if (node != null && !node.isNull() && !node.isMissingNode()) {
            factors.add(new FactorPayload(category, node));
        }
    }

    private List<RiskPayload> collectTopLevelRiskPayloads(JsonNode json) {
        List<RiskPayload> risks = new ArrayList<>();
        addTopLevelRiskArray(risks, json, "risk_assessments");
        addTopLevelRiskArray(risks, json, "risks");
        return risks;
    }

    private void addTopLevelRiskArray(List<RiskPayload> risks, JsonNode json, String field) {
        for (JsonNode node : asNodes(json.get(field))) {
            risks.add(new RiskPayload(null, node));
        }
    }

    private Long insertContextFactor(ImportItem item, Long matchId, FactorPayload factor) {
        JsonNode node = factor.node();
        String category = normalizedCode(fallback(text(node, "category", "factor_category"), factor.defaultCategory()), "OTHER");
        String type = normalizedCode(fallback(text(node, "type", "factor_type", "key"), "RAW"), "RAW");
        String title = truncate(fallback(text(node, "title", "name", "headline"), category + ":" + type), 300);
        return insertAndReturnId(
                "INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_url, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                item.getId(), matchId,
                category,
                type,
                title,
                fallback(text(node, "summary", "note", "description", "content"), toJson(node)),
                normalizedCode(text(node, "impact_direction", "impact", "direction", "sentiment_label"), "UNKNOWN"),
                normalizedCode(text(node, "entity_type"), "MATCH"),
                text(node, "entity_key", "entity", "team", "player"),
                normalizedCode(text(node, "evidence_level", "tier", "source_tier"), "UNKNOWN"),
                truncate(fallback(text(node, "source_name", "source"), "UNKNOWN"), 240),
                text(node, "source_url", "url"),
                text(node, "source_ref", "ref", "id"),
                parseDateTime(text(node, "observed_at", "captured_at", "time", "timestamp")),
                parseDateTime(text(node, "expires_at", "valid_until", "stale_at")),
                decimal(node, "confidence_score", "confidence"),
                decimal(node, "reliability_score", "reliability", "score"),
                toJson(node)
        );
    }

    private Long insertRiskAssessment(ImportItem item, Long matchId, RiskPayload risk) {
        JsonNode node = risk.node();
        String type = normalizedCode(fallback(text(node, "type", "risk_type"), "RAW_RISK"), "RAW_RISK");
        String level = normalizedCode(text(node, "level", "risk_level"), "UNKNOWN");
        return insertAndReturnId(
                "INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                item.getId(), matchId, risk.factorId(),
                type,
                level,
                decimal(node, "score", "risk_score"),
                truncate(fallback(text(node, "title", "name"), type), 300),
                fallback(text(node, "rationale", "summary", "reason", "description"), toJson(node)),
                normalizedCode(text(node, "suggested_action", "action"), "MONITOR"),
                truncate(fallback(text(node, "source_name", "source"), "UNKNOWN"), 240),
                text(node, "source_ref", "ref", "id"),
                toJson(node)
        );
    }

    private String normalizedCode(String value, String fallback) {
        String safe = fallback(value, fallback);
        return safe == null ? null : safe.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
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

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace("Z", "");
        if (normalized.length() >= 19) {
            normalized = normalized.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private BigDecimal decimalValue(JsonNode node) {
        String value = textValue(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException cause) {
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
