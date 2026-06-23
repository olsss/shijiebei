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
import java.math.RoundingMode;
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

    private record BetPlanPayload(String defaultKey, JsonNode node) {
    }

    public List<CoreDataMappingResponse> map(ImportItem item, JsonNode json, String actor) {
        if (item.getItemType() == ImportItemType.TEAM) {
            return mapTeam(item, json);
        }
        if (item.getItemType() == ImportItemType.PLAYER) {
            return mapPlayer(item, json);
        }
        if (item.getItemType() == ImportItemType.MATCH) {
            return mapMatch(item, json);
        }
        if (item.getItemType() == ImportItemType.MATCH_LINEUP) {
            return mapMatchLineup(item, json);
        }
        if (item.getItemType() == ImportItemType.MATCH_EVENT) {
            return mapMatchEvent(item, json);
        }
        if (item.getItemType() == ImportItemType.MATCH_STATS) {
            return mapMatchStats(item, json);
        }
        if (item.getItemType() == ImportItemType.BET_PLAN) {
            return mapBetPlan(item, json);
        }
        if (item.getItemType() == ImportItemType.BET) {
            return mapBets(item, json);
        }
        if (item.getItemType() == ImportItemType.POST_REVIEW) {
            return mapPostReview(item, json);
        }
        if (item.getItemType() == ImportItemType.REVIEW_LESSON) {
            return mapReviewLesson(item, json);
        }
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

    private List<CoreDataMappingResponse> mapTeam(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        String teamKey = truncate(required(text(payload, "team_key", "key", "id"), "team_key 不能为空"), 240);
        String displayName = truncate(fallback(text(payload, "display_name", "name", "team_name"), teamKey), 240);
        List<Long> existing = jdbcTemplate.query("SELECT id FROM teams WHERE team_key=?", (rs, rowNum) -> rs.getLong("id"), teamKey);
        Long teamId;
        if (existing.isEmpty()) {
            teamId = insertAndReturnId(
                    "INSERT INTO teams(team_key, display_name, fifa_code, country_region, style_tags, attack_profile, defense_profile, public_sentiment, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                    teamKey,
                    displayName,
                    truncate(text(payload, "fifa_code", "fifaCode"), 20),
                    truncate(text(payload, "country_region", "region"), 120),
                    textOrJson(payload, "style_tags", "tags"),
                    text(payload, "attack_profile", "attackProfile"),
                    text(payload, "defense_profile", "defenseProfile"),
                    text(payload, "public_sentiment", "sentiment"),
                    toJson(json)
            );
        } else {
            teamId = existing.get(0);
            jdbcTemplate.update(
                    "UPDATE teams SET display_name=?, fifa_code=COALESCE(?, fifa_code), country_region=COALESCE(?, country_region), style_tags=COALESCE(?, style_tags), attack_profile=COALESCE(?, attack_profile), defense_profile=COALESCE(?, defense_profile), public_sentiment=COALESCE(?, public_sentiment), raw_payload=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    displayName,
                    truncate(text(payload, "fifa_code", "fifaCode"), 20),
                    truncate(text(payload, "country_region", "region"), 120),
                    textOrJson(payload, "style_tags", "tags"),
                    text(payload, "attack_profile", "attackProfile"),
                    text(payload, "defense_profile", "defenseProfile"),
                    text(payload, "public_sentiment", "sentiment"),
                    toJson(json),
                    teamId
            );
        }
        return List.of(insertMapping(item.getId(), "TEAM", teamId, "IMPORTED", "球队主数据已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapPlayer(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        String playerKey = truncate(required(text(payload, "player_key", "key", "id"), "player_key 不能为空"), 240);
        String displayName = truncate(fallback(text(payload, "display_name", "name", "player_name"), playerKey), 240);
        Long teamId = resolveTeamIdIfProvided(payload, "team_key does not exist for player", "team_key", "team");
        List<Long> existing = jdbcTemplate.query("SELECT id FROM players WHERE player_key=?", (rs, rowNum) -> rs.getLong("id"), playerKey);
        Long playerId;
        if (existing.isEmpty()) {
            playerId = insertAndReturnId(
                    "INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    playerKey,
                    teamId,
                    displayName,
                    integer(payload, "shirt_number", "number"),
                    truncate(text(payload, "position"), 80),
                    truncate(text(payload, "status"), 80),
                    truncate(text(payload, "injury_status", "injury"), 240),
                    truncate(text(payload, "card_status", "cards"), 240),
                    truncate(text(payload, "locker_room_status", "locker_room"), 500),
                    toJson(json)
            );
        } else {
            playerId = existing.get(0);
            jdbcTemplate.update(
                    "UPDATE players SET team_id=COALESCE(?, team_id), display_name=?, shirt_number=COALESCE(?, shirt_number), position=COALESCE(?, position), status=COALESCE(?, status), injury_status=COALESCE(?, injury_status), card_status=COALESCE(?, card_status), locker_room_status=COALESCE(?, locker_room_status), raw_payload=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    teamId,
                    displayName,
                    integer(payload, "shirt_number", "number"),
                    truncate(text(payload, "position"), 80),
                    truncate(text(payload, "status"), 80),
                    truncate(text(payload, "injury_status", "injury"), 240),
                    truncate(text(payload, "card_status", "cards"), 240),
                    truncate(text(payload, "locker_room_status", "locker_room"), 500),
                    toJson(json),
                    playerId
            );
        }
        return List.of(insertMapping(item.getId(), "PLAYER", playerId, "IMPORTED", "球员主数据已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapMatch(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        Long matchId = upsertMatchFromPayload(payload, json);
        return List.of(insertMapping(item.getId(), "MATCH", matchId, "IMPORTED", "比赛主数据已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapMatchLineup(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        Long matchId = requiredId(findMatchId(payload), "比赛不存在，无法导入阵容");
        Long teamId = resolveTeamIdIfProvided(payload, "team_key does not exist for lineup", "team_key", "team");
        Long playerId = resolvePlayerIdIfProvided(payload, "player_key does not exist for lineup", "player_key", "player");
        Long lineupId = insertAndReturnId(
                "INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId,
                teamId,
                playerId,
                truncate(text(payload, "role"), 80),
                truncate(text(payload, "position"), 80),
                bool(payload, "is_starter", "starter")
        );
        return List.of(insertMapping(item.getId(), "MATCH_LINEUP", lineupId, "IMPORTED", "比赛阵容已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapMatchEvent(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        Long matchId = requiredId(findMatchId(payload), "比赛不存在，无法导入事件");
        Long teamId = resolveTeamIdIfProvided(payload, "team_key does not exist for event", "team_key", "team");
        Long playerId = resolvePlayerIdIfProvided(payload, "player_key does not exist for event", "player_key", "player");
        Long eventId = insertAndReturnId(
                "INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId,
                integer(payload, "event_minute", "minute"),
                truncate(required(text(payload, "event_type", "type"), "event_type 不能为空"), 120),
                teamId,
                playerId,
                toJson(json)
        );
        return List.of(insertMapping(item.getId(), "MATCH_EVENT", eventId, "IMPORTED", "比赛事件已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapMatchStats(ImportItem item, JsonNode json) {
        JsonNode payload = payload(json);
        Long matchId = requiredId(findMatchId(payload), "比赛不存在，无法导入技术统计");
        Long teamId = resolveTeamIdIfProvided(payload, "team_key does not exist for stats", "team_key", "team");
        Long statsId = insertAndReturnId(
                "INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId,
                teamId,
                truncate(fallback(text(payload, "stats_type", "type"), "IMPORTED"), 80),
                integer(payload, "goals_for"),
                integer(payload, "goals_against"),
                integer(payload, "first_goal_minute"),
                textOrJson(payload, "scoring_minutes"),
                toJson(json)
        );
        return List.of(insertMapping(item.getId(), "MATCH_STATS", statsId, "IMPORTED", "球队技术统计已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapAnalysis(ImportItem item, JsonNode json) {
        JsonNode root = payload(json);
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(root, "match", "match_name", "比赛"), item.getSummaryTitle()),
                text(root, "matchday", "date", "比赛日"),
                text(root, "jc_code", "竞彩编号"),
                json
        );
        String analysisId = fallback(fallback(text(root, "id", "analysis_id"), text(json, "idempotency_key")), "analysis-" + item.getId());
        Long reportId = insertAndReturnId(
                "INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                item.getId(), matchId, analysisId,
                text(root, "conclusion_type", "结论类型"),
                text(root, "confidence", "置信度"),
                nullableJson(root.get("risks")),
                nullableJson(root.get("recommended")),
                nullableJson(root.get("dimensions")),
                text(root, "narrative_md", "正文"),
                toJson(json)
        );
        mappings.add(insertMapping(item.getId(), "ANALYSIS_REPORT", reportId, "IMPORTED", "比赛分析已导入正式库"));

        JsonNode sources = root.get("sources");
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
        for (BetPlanPayload plan : collectBetPlanPayloads(root, analysisId)) {
            Long planId = insertBetPlan(item, reportId, matchId, analysisId, root, plan);
            mappings.add(insertMapping(item.getId(), "BET_PLAN", planId, "IMPORTED", "AI 下注方案已导入正式库"));
            int order = 0;
            for (JsonNode planItem : collectBetPlanItems(plan.node())) {
                Long planItemId = insertBetPlanItem(planId, matchId, plan.node(), planItem, order);
                mappings.add(insertMapping(item.getId(), "BET_PLAN_ITEM", planItemId, "IMPORTED", "AI 下注方案明细已导入正式库"));
                order++;
            }
        }
        int reviewIndex = 0;
        for (JsonNode review : collectPostMatchReviews(root)) {
            Long reviewId = insertPostMatchReview(item, reportId, matchId, analysisId, review, reviewIndex);
            mappings.add(insertMapping(item.getId(), "POST_MATCH_REVIEW", reviewId, "IMPORTED", "赛后复盘已导入正式库"));
            for (JsonNode lesson : collectReviewLessons(review)) {
                Long lessonId = insertReviewLesson(reviewId, lesson);
                mappings.add(insertMapping(item.getId(), "REVIEW_LESSON", lessonId, "IMPORTED", "复盘沉淀规则已导入正式库"));
            }
            reviewIndex++;
        }
        return mappings;
    }

    private List<CoreDataMappingResponse> mapBetPlan(ImportItem item, JsonNode json) {
        JsonNode plan = payload(json);
        Long matchId = resolveMatchId(plan, json, item.getSummaryTitle());
        String defaultKey = fallback(text(json, "idempotency_key"), "bet-plan-" + item.getId());
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long planId = insertBetPlan(item, null, matchId, defaultKey, plan, new BetPlanPayload(defaultKey, plan));
        mappings.add(insertMapping(item.getId(), "BET_PLAN", planId, "IMPORTED", "AI 下注方案已导入正式库"));
        int order = 0;
        for (JsonNode planItem : collectBetPlanItems(plan)) {
            Long planItemId = insertBetPlanItem(planId, matchId, plan, planItem, order);
            mappings.add(insertMapping(item.getId(), "BET_PLAN_ITEM", planItemId, "IMPORTED", "AI 下注方案明细已导入正式库"));
            order++;
        }
        return mappings;
    }

    private List<CoreDataMappingResponse> mapPostReview(ImportItem item, JsonNode json) {
        JsonNode review = payload(json);
        Long matchId = resolveMatchId(review, json, item.getSummaryTitle());
        String analysisId = fallback(text(review, "analysis_id", "analysisId"), "standalone-" + item.getId());
        Long reviewId = insertPostMatchReview(item, null, matchId, analysisId, review, 0);
        return List.of(insertMapping(item.getId(), "POST_MATCH_REVIEW", reviewId, "IMPORTED", "赛后复盘已导入正式库"));
    }

    private List<CoreDataMappingResponse> mapReviewLesson(ImportItem item, JsonNode json) {
        JsonNode lesson = payload(json);
        Long reviewId = requiredId(findPostReviewId(lesson), "复盘记录不存在，无法导入复盘规则");
        Long lessonId = insertReviewLesson(reviewId, lesson);
        return List.of(insertMapping(item.getId(), "REVIEW_LESSON", lessonId, "IMPORTED", "复盘沉淀规则已导入正式库"));
    }

    private List<BetPlanPayload> collectBetPlanPayloads(JsonNode json, String analysisId) {
        List<BetPlanPayload> plans = new ArrayList<>();
        addSingleBetPlan(plans, json, "bet_plan", analysisId);
        int index = 0;
        for (JsonNode plan : asNodes(json.get("bet_plans"))) {
            plans.add(new BetPlanPayload(analysisId + "-" + index, plan));
            index++;
        }
        addSingleBetPlan(plans, json, "recommended_plan", analysisId + "-recommended");
        return plans;
    }

    private void addSingleBetPlan(List<BetPlanPayload> plans, JsonNode json, String field, String defaultKey) {
        JsonNode node = json.get(field);
        if (node != null && !node.isNull() && !node.isMissingNode()) {
            plans.add(new BetPlanPayload(defaultKey, node));
        }
    }

    private Long insertBetPlan(ImportItem item,
                               Long reportId,
                               Long matchId,
                               String analysisId,
                               JsonNode analysisNode,
                               BetPlanPayload payload) {
        JsonNode node = payload.node();
        String planKey = truncate(fallback(text(node, "plan_key", "id", "key", "方案编号"), payload.defaultKey()), 160);
        Long existingPlanId = findExistingBetPlanId(planKey, matchId);
        if (existingPlanId != null) {
            return existingPlanId;
        }
        String title = truncate(fallback(text(node, "title", "name", "plan_title", "方案名称"), planKey), 300);
        String riskSummary = fallback(
                text(node, "risk_summary", "risk_note", "risk", "资金分配理由", "风险说明"),
                nullableJson(firstPresent(node, "risks", "risk_points"))
        );
        return insertAndReturnId(
                "INSERT INTO bet_plans(import_item_id, analysis_report_id, match_id, plan_key, plan_title, conclusion_type, confidence, budget_amount, risk_summary, betting_method, strategy_type, status, generated_by, generated_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                item.getId(), reportId, matchId,
                planKey,
                title,
                fallback(text(node, "conclusion_type", "结论类型"), text(analysisNode, "conclusion_type", "结论类型")),
                fallback(text(node, "confidence", "置信度"), text(analysisNode, "confidence", "置信度")),
                decimal(node, "budget_amount", "budget", "预算"),
                riskSummary,
                truncate(text(node, "betting_method", "bettingMethod", "method", "下注方式"), 160),
                truncate(text(node, "strategy_type", "strategy", "策略类型"), 160),
                normalizedCode(text(node, "status", "状态"), "IMPORTED"),
                truncate(text(node, "generated_by", "generator", "生成者"), 160),
                parseDateTime(text(node, "generated_at", "created_at", "生成时间")),
                toJson(node)
        );
    }

    private List<JsonNode> collectBetPlanItems(JsonNode plan) {
        List<JsonNode> items = new ArrayList<>();
        addNodes(items, plan, "items");
        addNodes(items, plan, "selections");
        addNodes(items, plan, "tickets");
        return items;
    }

    private Long insertBetPlanItem(Long planId, Long matchId, JsonNode planNode, JsonNode itemNode, int order) {
        String marketType = marketCode(itemNode);
        String selection = fallback(text(itemNode, "selection", "selection_text", "选择", "投注项", "name", "label"), toJson(itemNode));
        Long existingItemId = findFirstId(
                "SELECT id FROM bet_plan_items WHERE bet_plan_id=? AND item_order=? ORDER BY id",
                planId,
                order
        );
        if (existingItemId != null) {
            return existingItemId;
        }
        return insertAndReturnId(
                "INSERT INTO bet_plan_items(bet_plan_id, match_id, market_type, selection_text, stake_suggestion, odds, line_value, logic_type, risk_level, play_type, pass_type, item_order, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                planId,
                matchId,
                marketType,
                truncate(selection, 500),
                decimal(itemNode, "stake_suggestion", "stake", "amount", "投注额", "金额建议"),
                decimal(itemNode, "odds", "price", "赔率"),
                truncate(text(itemNode, "line_value", "line", "handicap", "盘口", "让球"), 120),
                truncate(normalizedCode(text(itemNode, "logic_type", "logic", "逻辑类型"), "RAW"), 120),
                truncate(normalizedCode(text(itemNode, "risk_level", "risk", "风险等级"), "UNKNOWN"), 80),
                truncate(fallback(text(itemNode, "play_type", "玩法类型", "下注方式"), text(planNode, "betting_method", "下注方式")), 160),
                truncate(fallback(text(itemNode, "pass_type", "过关方式", "串关"), text(planNode, "pass_type", "过关方式", "串关")), 160),
                order,
                toJson(itemNode)
        );
    }

    private List<JsonNode> collectPostMatchReviews(JsonNode json) {
        List<JsonNode> reviews = new ArrayList<>();
        addNodes(reviews, json, "post_match_review");
        addNodes(reviews, json, "review");
        addNodes(reviews, json, "post_match_reviews");
        return reviews;
    }

    private Long insertPostMatchReview(ImportItem item, Long reportId, Long matchId, String analysisId, JsonNode review, int index) {
        String reviewKey = truncate(fallback(text(review, "review_key", "id", "key", "复盘编号"), "review-" + analysisId + "-" + index), 160);
        Long existingReviewId = findExistingPostMatchReviewId(reviewKey, matchId);
        if (existingReviewId != null) {
            return existingReviewId;
        }
        String title = truncate(fallback(text(review, "title", "name", "review_title", "复盘标题"), reviewKey), 300);
        return insertAndReturnId(
                "INSERT INTO post_match_reviews(import_item_id, match_id, analysis_report_id, review_key, review_title, math_review, football_review, handicap_review, tournament_temperament_review, odds_value_review, overall_summary, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                item.getId(),
                matchId,
                reportId,
                reviewKey,
                title,
                text(review, "math_review", "数学层", "math"),
                text(review, "football_review", "足球层", "football"),
                text(review, "handicap_review", "盘口层", "handicap"),
                text(review, "tournament_temperament_review", "大赛气质层", "temperament"),
                text(review, "odds_value_review", "赔率价值层", "clv_review"),
                text(review, "overall_summary", "summary", "总评"),
                toJson(review)
        );
    }

    private List<JsonNode> collectReviewLessons(JsonNode review) {
        List<JsonNode> lessons = new ArrayList<>();
        addNodes(lessons, review, "lessons");
        addNodes(lessons, review, "rules");
        return lessons;
    }

    private Long insertReviewLesson(Long reviewId, JsonNode lesson) {
        String lessonText = fallback(text(lesson, "text", "lesson_text", "rule", "content", "summary", "规则"), toJson(lesson));
        String lessonType = truncate(normalizedCode(text(lesson, "type", "lesson_type", "\u89c4\u5219\u7c7b\u578b"), "GENERAL"), 120);
        Long existingLessonId = findFirstId(
                "SELECT id FROM review_lessons WHERE review_id=? AND lesson_type=? AND lesson_text=? ORDER BY id",
                reviewId,
                lessonType,
                lessonText
        );
        if (existingLessonId != null) {
            return existingLessonId;
        }
        return insertAndReturnId(
                "INSERT INTO review_lessons(review_id, lesson_type, lesson_text, severity, raw_payload) VALUES (?,?,?,?,?)",
                reviewId,
                lessonType,
                lessonText,
                truncate(normalizedCode(text(lesson, "severity", "level", "严重性"), "INFO"), 80),
                toJson(lesson)
        );
    }

    private List<CoreDataMappingResponse> mapOdds(ImportItem item, JsonNode json) {
        JsonNode root = payload(json);
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(root, "match", "match_name", "比赛"), item.getSummaryTitle()),
                text(root, "matchday", "date", "date_beijing", "比赛日"),
                text(root, "jc_code", "竞彩编号"),
                json
        );

        List<OddsPayload> oddsPayloads = collectOddsPayloads(root);
        if (oddsPayloads.isEmpty()) {
            oddsPayloads = List.of(new OddsPayload(fallback(text(root, "source", "bookmaker"), "UNKNOWN"), root));
        }
        for (OddsPayload payload : oddsPayloads) {
            JsonNode oddsNode = payload.node();
            String bookmaker = fallback(payload.bookmaker(), fallback(text(oddsNode, "name", "bookmaker", "company"), fallback(text(root, "source", "bookmaker"), "UNKNOWN")));
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
        JsonNode root = payload(json);
        List<CoreDataMappingResponse> mappings = new ArrayList<>();
        Long matchId = upsertMatch(
                fallback(text(root, "match", "match_name", "\u6bd4\u8d5b"), item.getSummaryTitle()),
                text(root, "matchday", "date", "\u6bd4\u8d5b\u65e5"),
                text(root, "jc_code", "\u7ade\u5f69\u7f16\u53f7"),
                json
        );

        List<JsonNode> snapshots = asNodes(root.get("snapshots"));
        if (snapshots.isEmpty()) {
            snapshots = List.of(root);
        }
        for (JsonNode snapshot : snapshots) {
            Long evidenceId = insertAndReturnId(
                    "INSERT INTO source_evidence(import_item_id, match_id, source_type, source_name, source_ref, source_url, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                    item.getId(), matchId,
                    fallback(text(snapshot, "type", "source_type"), "SOURCE_SNAPSHOT"),
                    fallback(text(snapshot, "name", "title", "source"), fallback(text(root, "source", "title"), "UNKNOWN")),
                    text(snapshot, "id", "ref", "source_ref"),
                    text(snapshot, "url", "source_url"),
                    fallback(text(snapshot, "summary", "note", "title"), toJson(snapshot)),
                    decimal(snapshot, "reliability", "score"),
                    toJson(snapshot)
            );
            mappings.add(insertMapping(item.getId(), "SOURCE_EVIDENCE", evidenceId, "IMPORTED", "Source evidence imported"));
        }

        for (JsonNode conflict : asNodes(root.get("conflicts"))) {
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
            mappings.add(insertMapping(item.getId(), "DATA_CONFLICT", conflictId, "IMPORTED", "Data conflict imported"));
        }

        for (FactorPayload factor : collectFactorPayloads(root)) {
            Long factorId = insertContextFactor(item, matchId, factor);
            mappings.add(insertMapping(item.getId(), "MATCH_CONTEXT_FACTOR", factorId, "IMPORTED", "Context factor imported"));
            for (JsonNode risk : asNodes(factor.node().get("risks"))) {
                Long riskId = insertRiskAssessment(item, matchId, new RiskPayload(factorId, risk));
                mappings.add(insertMapping(item.getId(), "SENTIMENT_RISK_ASSESSMENT", riskId, "IMPORTED", "Risk assessment imported"));
            }
        }
        for (RiskPayload risk : collectTopLevelRiskPayloads(root)) {
            Long riskId = insertRiskAssessment(item, matchId, risk);
            mappings.add(insertMapping(item.getId(), "SENTIMENT_RISK_ASSESSMENT", riskId, "IMPORTED", "Risk assessment imported"));
        }
        importAliases(root);
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
        JsonNode root = payload(json);
        List<JsonNode> bets = asNodes(root.get("bets"));
        if (bets.isEmpty()) {
            bets = List.of(root);
        }
        int index = 0;
        for (JsonNode bet : bets) {
            String matchName = fallback(text(bet, "match", "match_name", "比赛"), fallback(text(root, "match", "比赛"), item.getSummaryTitle()));
            String matchday = fallback(text(bet, "matchday", "date", "比赛日"), text(root, "matchday", "date", "比赛日"));
            String jcCode = fallback(text(bet, "jc_code", "竞彩编号"), text(root, "jc_code", "竞彩编号"));
            Long matchId = upsertMatch(matchName, matchday, jcCode, bet);
            String betId = fallback(text(bet, "bet_id", "id", "编号"), "bet-" + item.getId() + "-" + index);
            Long existingBetId = findFirstId("SELECT id FROM bets WHERE bet_id=? ORDER BY id", betId);
            if (existingBetId != null) {
                mappings.add(insertMapping(item.getId(), "BET", existingBetId, "IMPORTED", "Existing bet row reused"));
                index++;
                continue;
            }
            BigDecimal odds = decimal(bet, "odds", "赔率");
            BigDecimal closingOdds = decimal(bet, "closing_odds", "closingOdds", "收盘赔率");
            BigDecimal clv = calculateClv(odds, closingOdds, decimal(bet, "clv", "CLV"));
            Long targetId = insertAndReturnId(
                    "INSERT INTO bets(import_item_id, match_id, bet_id, ticket_no, bet_date, matchday, match_name, market_type, selection_text, stake, odds, closing_odds, clv, return_amount, hit_status, profit_loss, settled_at, review_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    item.getId(), matchId, betId,
                    text(bet, "ticket_no", "ticket", "票号"),
                    parseDate(fallback(text(bet, "bet_date", "下注日期"), text(root, "bet_date", "下注日期"))),
                    parseDate(matchday),
                    matchName,
                    text(bet, "market", "market_type", "玩法"),
                    text(bet, "selection", "selection_text", "选择", "投注项"),
                    decimal(bet, "stake", "投注额", "本金"),
                    odds,
                    closingOdds,
                    clv,
                    decimal(bet, "return_amount", "return", "返还"),
                    fallback(text(bet, "hit_status", "命中", "status"), "PENDING"),
                    decimal(bet, "profit_loss", "盈亏"),
                    parseDateTime(text(bet, "settled_at", "settledAt", "结算时间")),
                    normalizedCode(text(bet, "review_status", "复盘状态"), "UNREVIEWED"),
                    toJson(bet)
            );
            mappings.add(insertMapping(item.getId(), "BET", targetId, "IMPORTED", "下注记录已导入正式库"));
            index++;
        }
        return mappings;
    }

    private Long resolveMatchId(JsonNode payload, JsonNode rawPayload, String fallbackSummary) {
        Long existingMatchId = findMatchId(payload);
        if (existingMatchId != null) {
            return existingMatchId;
        }
        String matchName = fallback(text(payload, "match_name", "match", "比赛"), fallbackSummary);
        String matchday = text(payload, "matchday", "date", "比赛日");
        String jcCode = text(payload, "jc_code", "竞彩编号");
        return upsertMatch(matchName, matchday, jcCode, rawPayload);
    }

    private Long upsertMatch(String matchName, String matchday, String jcCode, JsonNode rawPayload) {
        String safeMatchName = fallback(matchName, "UNKNOWN_MATCH");
        String explicitKey = text(payload(rawPayload), "match_key");
        String key = fallback(explicitKey, matchKeyNormalizer.normalize(safeMatchName, matchday, jcCode));
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

    private Long upsertMatchFromPayload(JsonNode payload, JsonNode rawPayload) {
        String incomingMatchName = text(payload, "match_name", "match", "\u6bd4\u8d5b");
        String matchName = fallback(incomingMatchName, "UNKNOWN_MATCH");
        String matchday = text(payload, "matchday", "date", "\u6bd4\u8d5b\u65e5");
        String jcCode = text(payload, "jc_code", "\u7ade\u5f69\u7f16\u53f7");
        String matchKey = fallback(text(payload, "match_key"), matchKeyNormalizer.normalize(matchName, matchday, jcCode));
        Long homeTeamId = findTeamId(text(payload, "home_team_key", "home_team", "home"));
        Long awayTeamId = findTeamId(text(payload, "away_team_key", "away_team", "away"));
        List<Long> existing = jdbcTemplate.query("SELECT id FROM matches WHERE match_key=?", (rs, rowNum) -> rs.getLong("id"), matchKey);
        if (existing.isEmpty()) {
            insertAndReturnId(
                    "INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    matchKey,
                    matchName,
                    parseDate(matchday),
                    jcCode,
                    truncate(text(payload, "competition"), 160),
                    truncate(text(payload, "stage"), 120),
                    truncate(text(payload, "venue"), 240),
                    parseDateTime(text(payload, "kickoff_time", "kickoff", "commence_time")),
                    homeTeamId,
                    awayTeamId,
                    fallback(text(payload, "status"), "IMPORTED"),
                    fallback(text(payload, "result_status"), "UNKNOWN"),
                    nullableJson(payload.get("external_factors")),
                    toJson(rawPayload)
            );
            return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, matchKey);
        }
        Long matchId = existing.get(0);
        jdbcTemplate.update(
                "UPDATE matches SET match_name=COALESCE(?, match_name), matchday=COALESCE(?, matchday), jc_code=COALESCE(?, jc_code), competition=COALESCE(?, competition), stage=COALESCE(?, stage), venue=COALESCE(?, venue), kickoff_time=COALESCE(?, kickoff_time), home_team_id=COALESCE(?, home_team_id), away_team_id=COALESCE(?, away_team_id), status=COALESCE(?, status), result_status=COALESCE(?, result_status), external_factors=COALESCE(?, external_factors), raw_payload=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                incomingMatchName,
                parseDate(matchday),
                jcCode,
                truncate(text(payload, "competition"), 160),
                truncate(text(payload, "stage"), 120),
                truncate(text(payload, "venue"), 240),
                parseDateTime(text(payload, "kickoff_time", "kickoff", "commence_time")),
                homeTeamId,
                awayTeamId,
                text(payload, "status"),
                text(payload, "result_status"),
                nullableJson(payload.get("external_factors")),
                toJson(rawPayload),
                matchId
        );
        return matchId;
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

    private BigDecimal calculateClv(BigDecimal entryOdds, BigDecimal closingOdds, BigDecimal explicitClv) {
        if (explicitClv != null) {
            return explicitClv.setScale(6, RoundingMode.HALF_UP);
        }
        if (entryOdds == null || closingOdds == null || closingOdds.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return entryOdds.divide(closingOdds, 6, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private Long findExistingBetPlanId(String planKey, Long matchId) {
        if (matchId != null) {
            Long existingForMatch = findFirstId(
                    "SELECT id FROM bet_plans WHERE plan_key=? AND match_id=? ORDER BY id",
                    planKey,
                    matchId
            );
            if (existingForMatch != null) {
                return existingForMatch;
            }
        }
        return findFirstId("SELECT id FROM bet_plans WHERE plan_key=? ORDER BY id", planKey);
    }

    private Long findExistingPostMatchReviewId(String reviewKey, Long matchId) {
        if (matchId != null) {
            Long existingForMatch = findFirstId(
                    "SELECT id FROM post_match_reviews WHERE review_key=? AND match_id=? ORDER BY id",
                    reviewKey,
                    matchId
            );
            if (existingForMatch != null) {
                return existingForMatch;
            }
        }
        return findFirstId("SELECT id FROM post_match_reviews WHERE review_key=? ORDER BY id", reviewKey);
    }

    private Long findFirstId(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), args);
        return ids.isEmpty() ? null : ids.get(0);
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

    private void addNodes(List<JsonNode> target, JsonNode parent, String field) {
        target.addAll(asNodes(parent.get(field)));
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

    private JsonNode payload(JsonNode json) {
        JsonNode payload = firstPresent(json, "payload");
        return payload != null && payload.isObject() ? payload : json;
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

    private Integer integer(JsonNode node, String... fields) {
        String value = text(node, fields);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException cause) {
            return null;
        }
    }

    private Long resolveTeamIdIfProvided(JsonNode payload, String message, String... fields) {
        String teamKey = text(payload, fields);
        Long teamId = findTeamId(teamKey);
        if (teamKey != null && teamId == null) {
            throw new IllegalArgumentException(message + ": " + teamKey);
        }
        return teamId;
    }

    private Long resolvePlayerIdIfProvided(JsonNode payload, String message, String... fields) {
        String playerKey = text(payload, fields);
        Long playerId = findPlayerId(playerKey);
        if (playerKey != null && playerId == null) {
            throw new IllegalArgumentException(message + ": " + playerKey);
        }
        return playerId;
    }

    private Long findTeamId(String teamKey) {
        if (teamKey == null || teamKey.isBlank()) {
            return null;
        }
        List<Long> ids = jdbcTemplate.query("SELECT id FROM teams WHERE team_key=?", (rs, rowNum) -> rs.getLong("id"), teamKey);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findPlayerId(String playerKey) {
        if (playerKey == null || playerKey.isBlank()) {
            return null;
        }
        List<Long> ids = jdbcTemplate.query("SELECT id FROM players WHERE player_key=?", (rs, rowNum) -> rs.getLong("id"), playerKey);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findMatchId(JsonNode payload) {
        String matchKey = text(payload, "match_key");
        if (matchKey != null && !matchKey.isBlank()) {
            List<Long> ids = jdbcTemplate.query("SELECT id FROM matches WHERE match_key=?", (rs, rowNum) -> rs.getLong("id"), matchKey);
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
        }
        String matchName = text(payload, "match_name", "match", "比赛");
        if (matchName == null || matchName.isBlank()) {
            return null;
        }
        String key = matchKeyNormalizer.normalize(matchName, text(payload, "matchday", "date", "比赛日"), text(payload, "jc_code", "竞彩编号"));
        List<Long> ids = jdbcTemplate.query("SELECT id FROM matches WHERE match_key=?", (rs, rowNum) -> rs.getLong("id"), key);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findPostReviewId(JsonNode payload) {
        String reviewKey = text(payload, "review_key", "id", "key");
        if (reviewKey == null || reviewKey.isBlank()) {
            return null;
        }
        Long matchId = findMatchId(payload);
        List<Long> ids;
        if (matchId != null) {
            ids = jdbcTemplate.query(
                    "SELECT id FROM post_match_reviews WHERE review_key=? AND match_id=? ORDER BY id DESC",
                    (rs, rowNum) -> rs.getLong("id"),
                    reviewKey,
                    matchId
            );
        } else {
            ids = jdbcTemplate.query(
                    "SELECT id FROM post_match_reviews WHERE review_key=? ORDER BY id DESC",
                    (rs, rowNum) -> rs.getLong("id"),
                    reviewKey
            );
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String textOrJson(JsonNode node, String... fields) {
        JsonNode value = firstPresent(node, fields);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return toJson(value);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Long requiredId(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Boolean bool(JsonNode node, String... fields) {
        JsonNode value = firstPresent(node, fields);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = textValue(value);
        return text != null && ("true".equalsIgnoreCase(text.trim()) || "1".equals(text.trim()) || "yes".equalsIgnoreCase(text.trim()));
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
