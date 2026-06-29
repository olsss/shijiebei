package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchAnalysisReport;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchConflict;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchEvidence;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchFact;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchIntegrityCheck;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchMatchSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchOddsMarket;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchOddsSelection;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchPlayer;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchSentimentFactor;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchSentimentRisk;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchTeam;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchTeamComparison;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchVisualSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicScoreboard;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamVisual;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicVisualMetric;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicPrematchWorkbenchService {
    private static final int DEFAULT_LIMIT = 50;
    private static final String APPROVED_FACT_CONDITION = "approved_by IS NOT NULL AND approved_by <> ''";
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:比分\\s*[=：:]?\\s*)?(\\d{1,2})\\s*[-:：比]\\s*(\\d{1,2})");

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicPrematchWorkbenchService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicPrematchMatchSummary> matches() {
        LocalDateTime staleLiveOddsCutoff = staleLiveOddsCutoff();
        return jdbcTemplate.query(summarySelect() + """
                ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC
                LIMIT ?
                """, (rs, rowNum) -> summaryRow(rs).summary(), staleLiveOddsCutoff, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public PublicPrematchDetail match(long matchId) {
        SummaryRow summary = findSummary(matchId);
        return new PublicPrematchDetail(
                summary.summary(),
                visualSummary(summary),
                teamComparison(summary),
                teams(matchId),
                lineups(matchId),
                players(matchId),
                oddsMarkets(matchId),
                sentimentFactors(matchId),
                evidence(matchId),
                conflicts(matchId),
                analysisReports(matchId),
                integrityChecks(summary)
        );
    }

    @Transactional(readOnly = true)
    public List<PublicPrematchIntegrityCheck> integrity(long matchId) {
        return integrityChecks(findSummary(matchId));
    }

    private SummaryRow findSummary(long matchId) {
        LocalDateTime staleLiveOddsCutoff = staleLiveOddsCutoff();
        return jdbcTemplate.query(summarySelect() + " WHERE m.id=?", (rs, rowNum) -> summaryRow(rs), staleLiveOddsCutoff, matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private String summarySelect() {
        return """
                SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue,
                       m.kickoff_time, m.status, m.result_status, m.raw_payload AS match_raw_payload, m.home_team_id,
                       COALESCE(ht.display_name, '主队待定') AS home_team_name,
                       ht.fifa_code AS home_fifa_code, ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region,
                       m.away_team_id,
                       COALESCE(at.display_name, '客队待定') AS away_team_name,
                       at.fifa_code AS away_fifa_code, at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region,
                       (SELECT s.goals_for FROM match_team_stats s
                        WHERE s.match_id=m.id AND s.team_id=m.home_team_id
                        ORDER BY s.id DESC LIMIT 1) AS home_score,
                       (SELECT s.goals_for FROM match_team_stats s
                        WHERE s.match_id=m.id AND s.team_id=m.away_team_id
                        ORDER BY s.id DESC LIMIT 1) AS away_score,
                       (SELECT COUNT(*) FROM match_events e
                        WHERE e.match_id=m.id AND e.team_id=m.home_team_id
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score,
                       (SELECT COUNT(*) FROM match_events e
                        WHERE e.match_id=m.id AND e.team_id=m.away_team_id
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score,
                       (SELECT se.summary FROM source_evidence se
                        WHERE se.match_id=m.id AND se.summary IS NOT NULL
                        ORDER BY se.reliability_score DESC, se.id DESC LIMIT 1) AS score_evidence_summary,
                       (SELECT COUNT(*) FROM team_profile_facts tf
                        WHERE tf.team_id IN (m.home_team_id, m.away_team_id)
                          AND tf.approved_by IS NOT NULL AND tf.approved_by <> '') AS team_profile_count,
                       (SELECT COUNT(*)
                        FROM player_profile_facts pf
                        WHERE pf.approved_by IS NOT NULL AND pf.approved_by <> ''
                          AND EXISTS (
                            SELECT 1 FROM match_lineups ml
                            WHERE ml.match_id=m.id AND ml.player_id=pf.player_id
                        )) AS player_profile_count,
                       (SELECT COUNT(*) FROM match_lineups ml WHERE ml.match_id=m.id) AS lineup_count,
                       (SELECT COUNT(*) FROM odds_market_snapshots oms WHERE oms.match_id=m.id) AS odds_market_count,
                       (SELECT COUNT(*) FROM odds_market_snapshots oms
                        WHERE oms.match_id=m.id AND UPPER(oms.snapshot_type)='LIVE'
                          AND (oms.captured_at IS NULL OR oms.captured_at < ?)) AS stale_live_odds_count,
                       (SELECT COUNT(*) FROM match_context_factors mcf WHERE mcf.match_id=m.id) AS sentiment_factor_count,
                       (SELECT COUNT(*) FROM match_context_factors mcf
                        WHERE mcf.match_id=m.id AND mcf.expires_at IS NOT NULL AND mcf.expires_at < CURRENT_TIMESTAMP) AS stale_sentiment_count,
                       (SELECT COUNT(*) FROM analysis_reports ar WHERE ar.match_id=m.id) AS analysis_report_count,
                       (SELECT COUNT(*) FROM source_evidence se WHERE se.match_id=m.id) AS evidence_count,
                       (SELECT COUNT(*) FROM data_conflicts dc
                        WHERE dc.match_id=m.id AND (dc.resolution_status IS NULL OR dc.resolution_status <> 'RESOLVED')) AS unresolved_conflict_count
                FROM matches m
                LEFT JOIN teams ht ON ht.id=m.home_team_id
                LEFT JOIN teams at ON at.id=m.away_team_id
                """;
    }

    private LocalDateTime staleLiveOddsCutoff() {
        return LocalDateTime.now().minusHours(3);
    }

    private SummaryRow summaryRow(ResultSet rs) throws SQLException {
        long teamProfileCount = rs.getLong("team_profile_count");
        long playerProfileCount = rs.getLong("player_profile_count");
        long lineupCount = rs.getLong("lineup_count");
        long oddsMarketCount = rs.getLong("odds_market_count");
        long staleLiveOddsCount = rs.getLong("stale_live_odds_count");
        long sentimentFactorCount = rs.getLong("sentiment_factor_count");
        long staleSentimentCount = rs.getLong("stale_sentiment_count");
        long analysisReportCount = rs.getLong("analysis_report_count");
        long evidenceCount = rs.getLong("evidence_count");
        long unresolvedConflictCount = rs.getLong("unresolved_conflict_count");

        long missingCount = missingCount(teamProfileCount, playerProfileCount, lineupCount, oddsMarketCount,
                sentimentFactorCount, analysisReportCount, evidenceCount);
        long staleCount = staleLiveOddsCount + staleSentimentCount;
        int integrityScore = integrityScore(missingCount, staleCount, unresolvedConflictCount);

        PublicPrematchMatchSummary summary = new PublicPrematchMatchSummary(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("match_key")),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeText(rs.getString("competition")),
                mapper.sanitizeText(rs.getString("stage")),
                mapper.sanitizeText(rs.getString("venue")),
                localDateTime(rs, "kickoff_time"),
                mapper.sanitizeToken(rs.getString("status")),
                mapper.sanitizeToken(rs.getString("result_status")),
                nullableLong(rs, "home_team_id"),
                teamName(rs, "home_team_name", "home_team_name", "主队待定"),
                nullableLong(rs, "away_team_id"),
                teamName(rs, "away_team_name", "away_team_name", "客队待定"),
                teamVisual(
                        nullableLong(rs, "home_team_id"),
                        teamName(rs, "home_team_name", "home_team_name", "主队待定"),
                        rs.getString("home_fifa_code"),
                        rs.getString("home_country_iso2"),
                        rs.getString("home_flag_asset_key"),
                        rs.getString("home_country_region")
                ),
                teamVisual(
                        nullableLong(rs, "away_team_id"),
                        teamName(rs, "away_team_name", "away_team_name", "客队待定"),
                        rs.getString("away_fifa_code"),
                        rs.getString("away_country_iso2"),
                        rs.getString("away_flag_asset_key"),
                        rs.getString("away_country_region")
                ),
                scoreboard(
                        nullableInt(rs, "home_score"),
                        nullableInt(rs, "away_score"),
                        nullableInt(rs, "home_event_score"),
                        nullableInt(rs, "away_event_score"),
                        mapper.sanitizeText(rs.getString("score_evidence_summary")),
                        rs.getString("status"),
                        rs.getString("result_status"),
                        localDateTime(rs, "kickoff_time")
                ),
                integrityScore,
                missingCount,
                staleCount,
                unresolvedConflictCount,
                teamProfileCount,
                playerProfileCount,
                lineupCount,
                oddsMarketCount,
                sentimentFactorCount,
                analysisReportCount
        );
        return new SummaryRow(summary, teamProfileCount, playerProfileCount, lineupCount, oddsMarketCount,
                staleLiveOddsCount, sentimentFactorCount, staleSentimentCount, analysisReportCount,
                evidenceCount, unresolvedConflictCount);
    }

    private long missingCount(long teamProfileCount, long playerProfileCount, long lineupCount, long oddsMarketCount,
                              long sentimentFactorCount, long analysisReportCount, long evidenceCount) {
        long missing = 0;
        if (teamProfileCount == 0) missing++;
        if (playerProfileCount == 0) missing++;
        if (lineupCount == 0) missing++;
        if (oddsMarketCount == 0) missing++;
        if (sentimentFactorCount == 0) missing++;
        if (analysisReportCount == 0) missing++;
        if (evidenceCount < 2) missing++;
        return missing;
    }

    private int integrityScore(long missingCount, long staleCount, long conflictCount) {
        long failed = missingCount + staleCount + conflictCount;
        long totalChecks = 8;
        long passed = Math.max(0, totalChecks - failed);
        return (int) Math.round(passed * 100.0 / totalChecks);
    }

    private PublicPrematchVisualSummary visualSummary(SummaryRow row) {
        PublicPrematchMatchSummary summary = row.summary();
        String readinessText = summary.integrityScore() >= 85
                ? "公开资料较完整，比赛卡和分析摘要可读"
                : summary.integrityScore() >= 65
                ? "资料基本可读，但仍需核对缺失项和过期项"
                : "资料不足，建议先补证据再解读结论";
        String riskText = summary.conflictCount() > 0 || summary.staleCount() > 0
                ? "存在冲突或过期信息，赛前必须复核"
                : "暂无公开冲突，继续关注临场变化";
        String nextCheckText = row.lineupCount() == 0
                ? "下一步优先补官方首发/预计阵容"
                : row.oddsMarketCount() == 0
                ? "下一步优先补胜平负、让球、大小球等盘口"
                : row.sentimentFactorCount() == 0
                ? "下一步优先补伤停、天气、裁判、旅行休息等外部因素"
                : "下一步复核高风险证据和临场变化";
        return new PublicPrematchVisualSummary(
                mapper.sanitizeText(summary.scoreboard().resultText()),
                readinessText,
                riskText,
                nextCheckText,
                List.of(
                        metric("integrity", "资料准备度", BigDecimal.valueOf(summary.integrityScore()), "%", tone(summary.integrityScore()), "公开资料覆盖字段，反映球队、球员、赔率、舆情和证据覆盖"),
                        metric("missing", "缺失项", BigDecimal.valueOf(summary.missingCount()), "项", summary.missingCount() == 0 ? "success" : "warning", "缺失项数量，反映待补材料规模"),
                        metric("stale", "过期项", BigDecimal.valueOf(summary.staleCount()), "项", summary.staleCount() == 0 ? "success" : "warning", "赛前信息过期会误导判断"),
                        metric("conflict", "冲突项", BigDecimal.valueOf(summary.conflictCount()), "项", summary.conflictCount() == 0 ? "success" : "danger", "冲突需要人工核对来源"),
                        metric("odds_market", "赔率市场", BigDecimal.valueOf(row.oddsMarketCount()), "个", row.oddsMarketCount() > 0 ? "info" : "warning", "公开市场信号，仅含赔率与隐含概率字段"),
                        metric("sentiment_factor", "外部因素", BigDecimal.valueOf(row.sentimentFactorCount()), "条", row.sentimentFactorCount() > 0 ? "info" : "warning", "伤停、天气、裁判、旅行、战意等")
                )
        );
    }

    private List<PublicPrematchTeamComparison> teamComparison(SummaryRow row) {
        PublicPrematchMatchSummary summary = row.summary();
        List<PublicPrematchTeamComparison> comparisons = new ArrayList<>();
        if (summary.homeTeam() != null) {
            comparisons.add(teamComparison(summary.matchId(), summary.homeTeam()));
        }
        if (summary.awayTeam() != null) {
            comparisons.add(teamComparison(summary.matchId(), summary.awayTeam()));
        }
        return List.copyOf(comparisons);
    }

    private PublicPrematchTeamComparison teamComparison(long matchId, PublicTeamVisual team) {
        Long teamId = team.teamId();
        Integer goalsFor = null;
        Integer goalsAgainst = null;
        Integer firstGoalMinute = null;
        BigDecimal xg = null;
        BigDecimal xga = null;
        BigDecimal ppda = null;
        BigDecimal formScore = null;
        if (teamId != null) {
            List<Map<String, Object>> statRows = jdbcTemplate.queryForList("""
                    SELECT goals_for, goals_against, first_goal_minute
                    FROM match_team_stats
                    WHERE match_id=? AND team_id=?
                    ORDER BY id DESC
                    LIMIT 1
                    """, matchId, teamId);
            if (!statRows.isEmpty()) {
                Map<String, Object> stat = statRows.get(0);
                goalsFor = integerValue(stat.get("goals_for"));
                goalsAgainst = integerValue(stat.get("goals_against"));
                firstGoalMinute = integerValue(stat.get("first_goal_minute"));
            }
            List<Map<String, Object>> metricRows = jdbcTemplate.queryForList("""
                    SELECT xg, xga, ppda, form_score
                    FROM team_metric_snapshots
                    WHERE team_id=? AND (match_id=? OR match_id IS NULL)
                    ORDER BY CASE WHEN match_id=? THEN 0 ELSE 1 END, captured_at DESC, id DESC
                    LIMIT 1
                    """, teamId, matchId, matchId);
            if (!metricRows.isEmpty()) {
                Map<String, Object> metric = metricRows.get(0);
                xg = decimalValue(metric.get("xg"));
                xga = decimalValue(metric.get("xga"));
                ppda = decimalValue(metric.get("ppda"));
                formScore = decimalValue(metric.get("form_score"));
            }
        }
        return new PublicPrematchTeamComparison(team, List.of(
                metric("goals_for", "进球", number(goalsFor), "球", "success", "已入库比赛统计"),
                metric("goals_against", "失球", number(goalsAgainst), "球", "danger", "已入库比赛统计"),
                metric("first_goal_minute", "首球分钟", number(firstGoalMinute), "分", "info", "越早进球越可能影响比赛走势"),
                metric("xg", "预期进球 xG", xg, "", "accent", "衡量机会质量，待正式数据源补齐"),
                metric("xga", "预期失球 xGA", xga, "", "warning", "衡量被创造机会质量"),
                metric("ppda", "压迫强度 PPDA", ppda, "", "info", "压迫强度字段，供攻防画像展示"),
                metric("form_score", "近期状态", formScore, "分", "success", "综合近期表现的结构化快照")
        ));
    }

    private PublicVisualMetric metric(String key, String label, BigDecimal value, String unit, String tone, String explanation) {
        return new PublicVisualMetric(key, label, value, unit, tone, explanation);
    }

    private String tone(int score) {
        if (score >= 85) {
            return "success";
        }
        if (score >= 65) {
            return "warning";
        }
        return "danger";
    }

    private BigDecimal number(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private List<PublicPrematchTeam> teams(long matchId) {
        List<TeamRow> rows = jdbcTemplate.query("""
                SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags,
                       t.attack_profile, t.defense_profile, t.public_sentiment
                FROM matches m
                JOIN teams t ON t.id IN (m.home_team_id, m.away_team_id)
                WHERE m.id=?
                ORDER BY CASE WHEN t.id=m.home_team_id THEN 0 ELSE 1 END, t.display_name
                """, (rs, rowNum) -> new TeamRow(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("team_key")),
                mapper.sanitizeText(rs.getString("display_name")),
                mapper.sanitizeText(rs.getString("fifa_code")),
                mapper.sanitizeText(rs.getString("country_region")),
                mapper.sanitizeText(rs.getString("style_tags")),
                mapper.sanitizeText(rs.getString("attack_profile")),
                mapper.sanitizeText(rs.getString("defense_profile")),
                mapper.sanitizeText(rs.getString("public_sentiment"))
        ), matchId);
        Map<Long, List<PublicPrematchFact>> factsByTeamId = teamFactsByTeamIds(
                rows.stream().map(TeamRow::teamId).toList()
        );
        return rows.stream()
                .map(row -> new PublicPrematchTeam(
                        row.teamId(),
                        row.teamKey(),
                        row.teamName(),
                        row.fifaCode(),
                        row.countryRegion(),
                        row.styleTags(),
                        row.attackProfile(),
                        row.defenseProfile(),
                        row.publicSentiment(),
                        factsByTeamId.getOrDefault(row.teamId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PublicPrematchFact>> teamFactsByTeamIds(List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicPrematchFact>> grouped = new LinkedHashMap<>();
        String sql = """
                SELECT team_id, id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM team_profile_facts
                WHERE team_id IN (%s)
                  AND %s
                ORDER BY team_id, captured_at DESC, id DESC
                """.formatted(placeholders(teamIds.size()), APPROVED_FACT_CONDITION);
        jdbcTemplate.query(sql, rs -> {
            long teamId = rs.getLong("team_id");
            grouped.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(fact(rs));
        }, teamIds.toArray());
        return grouped;
    }

    private List<PublicPrematchLineup> lineups(long matchId) {
        return jdbcTemplate.query("""
                SELECT l.id, l.match_id, l.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name,
                       l.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name, l.role, l.position, l.is_starter
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                LEFT JOIN teams t ON t.id=l.team_id
                LEFT JOIN players p ON p.id=l.player_id
                WHERE l.match_id=?
                ORDER BY CASE WHEN l.team_id=m.home_team_id THEN 0 ELSE 1 END, l.is_starter DESC, p.shirt_number, p.display_name, l.id
                """, (rs, rowNum) -> new PublicPrematchLineup(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name")),
                mapper.sanitizeToken(rs.getString("role")),
                mapper.sanitizeText(rs.getString("position")),
                rs.getBoolean("is_starter")
        ), matchId);
    }

    private List<PublicPrematchPlayer> players(long matchId) {
        List<PlayerRow> rows = jdbcTemplate.query("""
                SELECT p.id, p.player_key, p.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name, p.display_name,
                       p.shirt_number, p.position, p.status, p.injury_status, p.card_status, p.locker_room_status
                FROM players p
                JOIN matches m ON m.id=?
                LEFT JOIN teams t ON t.id=p.team_id
                WHERE EXISTS (SELECT 1 FROM match_lineups l WHERE l.match_id=m.id AND l.player_id=p.id)
                ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, p.id
                """, (rs, rowNum) -> new PlayerRow(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("player_key")),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                mapper.sanitizeText(rs.getString("display_name")),
                nullableInt(rs, "shirt_number"),
                mapper.sanitizeText(rs.getString("position")),
                mapper.sanitizeToken(rs.getString("status")),
                mapper.sanitizeText(rs.getString("injury_status")),
                mapper.sanitizeText(rs.getString("card_status")),
                mapper.sanitizeText(rs.getString("locker_room_status"))
        ), matchId);
        Map<Long, List<PublicPrematchFact>> factsByPlayerId = playerFactsByPlayerIds(
                rows.stream().map(PlayerRow::playerId).toList()
        );
        return rows.stream()
                .map(row -> new PublicPrematchPlayer(
                        row.playerId(),
                        row.playerKey(),
                        row.teamId(),
                        row.teamName(),
                        row.playerName(),
                        row.shirtNumber(),
                        row.position(),
                        row.status(),
                        row.injuryStatus(),
                        row.cardStatus(),
                        row.lockerRoomStatus(),
                        factsByPlayerId.getOrDefault(row.playerId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PublicPrematchFact>> playerFactsByPlayerIds(List<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicPrematchFact>> grouped = new LinkedHashMap<>();
        String sql = """
                SELECT player_id, id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM player_profile_facts
                WHERE player_id IN (%s)
                  AND %s
                ORDER BY player_id, captured_at DESC, id DESC
                """.formatted(placeholders(playerIds.size()), APPROVED_FACT_CONDITION);
        jdbcTemplate.query(sql, rs -> {
            long playerId = rs.getLong("player_id");
            grouped.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(fact(rs));
        }, playerIds.toArray());
        return grouped;
    }

    private PublicPrematchFact fact(ResultSet rs) throws SQLException {
        return new PublicPrematchFact(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("fact_type")),
                mapper.sanitizeText(rs.getString("period_key")),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("summary")),
                mapper.sanitizeText(rs.getString("sentiment_label")),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_url")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "captured_at")
        );
    }

    private List<PublicPrematchOddsMarket> oddsMarkets(long matchId) {
        List<OddsMarketRow> rows = jdbcTemplate.query("""
                SELECT id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref
                FROM odds_market_snapshots
                WHERE match_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> new OddsMarketRow(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("bookmaker")),
                mapper.sanitizeToken(rs.getString("market_code")),
                mapper.sanitizeText(rs.getString("market_name")),
                mapper.sanitizeToken(rs.getString("snapshot_type")),
                rs.getBigDecimal("handicap_line"),
                mapper.sanitizeText(rs.getString("line_value")),
                localDateTime(rs, "captured_at"),
                mapper.sanitizeText(rs.getString("source_ref"))
        ), matchId);
        Map<Long, List<PublicPrematchOddsSelection>> selectionsByMarketId = oddsSelectionsByMarketIds(
                rows.stream().map(OddsMarketRow::marketId).toList()
        );
        return rows.stream()
                .map(row -> new PublicPrematchOddsMarket(
                        row.marketId(),
                        row.bookmaker(),
                        row.marketCode(),
                        row.marketName(),
                        row.snapshotType(),
                        row.handicapLine(),
                        row.lineValue(),
                        row.capturedAt(),
                        row.sourceRef(),
                        selectionsByMarketId.getOrDefault(row.marketId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PublicPrematchOddsSelection>> oddsSelectionsByMarketIds(List<Long> marketIds) {
        if (marketIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicPrematchOddsSelection>> grouped = new LinkedHashMap<>();
        String sql = """
                SELECT market_snapshot_id, id, selection_code, selection_name, odds_value, implied_probability, selection_status
                FROM odds_selection_snapshots
                WHERE market_snapshot_id IN (%s)
                ORDER BY market_snapshot_id, id
                """.formatted(placeholders(marketIds.size()));
        jdbcTemplate.query(sql, rs -> {
            long marketId = rs.getLong("market_snapshot_id");
            grouped.computeIfAbsent(marketId, ignored -> new ArrayList<>()).add(new PublicPrematchOddsSelection(
                    rs.getLong("id"),
                    mapper.sanitizeToken(rs.getString("selection_code")),
                    mapper.sanitizeText(rs.getString("selection_name")),
                    rs.getBigDecimal("odds_value"),
                    rs.getBigDecimal("implied_probability"),
                    mapper.sanitizeToken(rs.getString("selection_status"))
            ));
        }, marketIds.toArray());
        return grouped;
    }

    private List<PublicPrematchSentimentFactor> sentimentFactors(long matchId) {
        List<SentimentFactorRow> rows = jdbcTemplate.query("""
                SELECT id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key,
                       evidence_level, source_name, source_url, source_ref, observed_at, expires_at, confidence_score, reliability_score
                FROM match_context_factors
                WHERE match_id=?
                ORDER BY observed_at DESC, id DESC
                """, (rs, rowNum) -> new SentimentFactorRow(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeToken(rs.getString("factor_category")),
                mapper.sanitizeToken(rs.getString("factor_type")),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("summary")),
                mapper.sanitizeToken(rs.getString("impact_direction")),
                mapper.sanitizeToken(rs.getString("entity_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeText(rs.getString("evidence_level")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_url")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "observed_at"),
                localDateTime(rs, "expires_at"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score")
        ), matchId);
        Map<Long, List<PublicPrematchSentimentRisk>> risksByFactorId = sentimentRisksByFactorIds(
                rows.stream().map(SentimentFactorRow::factorId).toList()
        );
        return rows.stream()
                .map(row -> new PublicPrematchSentimentFactor(
                        row.factorId(),
                        row.matchId(),
                        row.factorCategory(),
                        row.factorType(),
                        row.title(),
                        row.summary(),
                        row.impactDirection(),
                        row.entityType(),
                        row.entityKey(),
                        row.evidenceLevel(),
                        row.sourceName(),
                        row.sourceUrl(),
                        row.sourceRef(),
                        row.observedAt(),
                        row.expiresAt(),
                        row.confidenceScore(),
                        row.reliabilityScore(),
                        risksByFactorId.getOrDefault(row.factorId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PublicPrematchSentimentRisk>> sentimentRisksByFactorIds(List<Long> factorIds) {
        if (factorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicPrematchSentimentRisk>> grouped = new LinkedHashMap<>();
        String sql = """
                SELECT factor_id, id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref
                FROM sentiment_risk_assessments
                WHERE factor_id IN (%s)
                ORDER BY factor_id, id DESC
                """.formatted(placeholders(factorIds.size()));
        jdbcTemplate.query(sql, rs -> {
            long factorId = rs.getLong("factor_id");
            grouped.computeIfAbsent(factorId, ignored -> new ArrayList<>()).add(new PublicPrematchSentimentRisk(
                    rs.getLong("id"),
                    mapper.sanitizeToken(rs.getString("risk_type")),
                    mapper.sanitizeToken(rs.getString("risk_level")),
                    rs.getBigDecimal("risk_score"),
                    mapper.sanitizeText(rs.getString("title")),
                    mapper.sanitizeText(rs.getString("rationale")),
                    mapper.sanitizeText(rs.getString("suggested_action")),
                    mapper.sanitizeText(rs.getString("source_name")),
                    mapper.sanitizeText(rs.getString("source_ref"))
            ));
        }, factorIds.toArray());
        return grouped;
    }

    private List<PublicPrematchEvidence> evidence(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score
                FROM source_evidence
                WHERE match_id=?
                ORDER BY evidence_time DESC, id
                """, (rs, rowNum) -> new PublicPrematchEvidence(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("source_type")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_ref")),
                mapper.sanitizeText(rs.getString("source_url")),
                localDateTime(rs, "evidence_time"),
                mapper.sanitizeText(rs.getString("summary")),
                rs.getBigDecimal("reliability_score")
        ), matchId);
    }

    private List<PublicPrematchConflict> conflicts(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, conflict_type, entity_key, field_name, resolution_status
                FROM data_conflicts
                WHERE match_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new PublicPrematchConflict(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("conflict_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeToken(rs.getString("field_name")),
                mapper.sanitizeToken(rs.getString("resolution_status"))
        ), matchId);
    }

    private List<PublicPrematchAnalysisReport> analysisReports(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions,
                       created_at, updated_at
                FROM analysis_reports
                WHERE match_id=?
                ORDER BY created_at DESC, id DESC
                """, (rs, rowNum) -> new PublicPrematchAnalysisReport(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("analysis_id")),
                mapper.sanitizeToken(rs.getString("conclusion_type")),
                mapper.sanitizeToken(rs.getString("confidence")),
                mapper.sanitizeText(rs.getString("risk_summary")),
                mapper.sanitizeText(rs.getString("recommended_markets")),
                mapper.sanitizeText(rs.getString("dimensions")),
                localDateTime(rs, "created_at"),
                localDateTime(rs, "updated_at")
        ), matchId);
    }

    private List<PublicPrematchIntegrityCheck> integrityChecks(SummaryRow row) {
        List<PublicPrematchIntegrityCheck> checks = new ArrayList<>();
        checks.add(check("TEAM_PROFILE", "Team profile", row.teamProfileCount() > 0, row.teamProfileCount()));
        checks.add(check("PLAYER_PROFILE", "Player profile", row.playerProfileCount() > 0, row.playerProfileCount()));
        checks.add(check("LINEUP", "Lineup", row.lineupCount() > 0, row.lineupCount()));
        checks.add(check("ODDS_MARKET", "Odds market", row.oddsMarketCount() > 0, row.oddsMarketCount()));
        checks.add(new PublicPrematchIntegrityCheck(
                "LIVE_ODDS_FRESHNESS",
                "Live odds freshness",
                row.staleLiveOddsCount() == 0 ? "PASS" : "STALE",
                row.staleLiveOddsCount() == 0 ? "INFO" : "MEDIUM",
                row.staleLiveOddsCount() == 0 ? "Live odds are fresh" : "Live odds are stale",
                row.oddsMarketCount(),
                null
        ));
        checks.add(new PublicPrematchIntegrityCheck(
                "SENTIMENT_FACTOR",
                "Sentiment factor",
                row.sentimentFactorCount() > 0 && row.staleSentimentCount() == 0 ? "PASS" : row.sentimentFactorCount() == 0 ? "MISSING" : "STALE",
                row.sentimentFactorCount() > 0 && row.staleSentimentCount() == 0 ? "INFO" : "HIGH",
                row.sentimentFactorCount() > 0 && row.staleSentimentCount() == 0 ? "Sentiment factors are fresh" : "Sentiment factors are missing or stale",
                row.sentimentFactorCount(),
                null
        ));
        checks.add(check("ANALYSIS_REPORT", "Analysis report", row.analysisReportCount() > 0, row.analysisReportCount()));
        checks.add(new PublicPrematchIntegrityCheck(
                "MULTI_SOURCE_EVIDENCE",
                "Multi-source evidence",
                row.evidenceCount() >= 2 ? "PASS" : "MISSING",
                row.evidenceCount() >= 2 ? "INFO" : "HIGH",
                row.evidenceCount() >= 2 ? "Multiple evidence sources are available" : "Multiple evidence sources are missing",
                row.evidenceCount(),
                null
        ));
        checks.add(new PublicPrematchIntegrityCheck(
                "UNRESOLVED_CONFLICT",
                "Unresolved conflict",
                row.unresolvedConflictCount() == 0 ? "PASS" : "CONFLICT",
                row.unresolvedConflictCount() == 0 ? "INFO" : "HIGH",
                row.unresolvedConflictCount() == 0 ? "No unresolved conflicts" : "Unresolved conflicts exist",
                row.unresolvedConflictCount(),
                null
        ));
        return List.copyOf(checks);
    }

    private PublicPrematchIntegrityCheck check(String code, String label, boolean pass, long evidenceCount) {
        return new PublicPrematchIntegrityCheck(
                code,
                label,
                pass ? "PASS" : "MISSING",
                pass ? "INFO" : "HIGH",
                pass ? "public check passed" : "public check missing data",
                evidenceCount,
                null
        );
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private PublicTeamVisual teamVisual(Long teamId, String teamName, String fifaCode, String countryIso2, String flagAssetKey, String countryRegion) {
        String sanitizedFifaCode = mapper.sanitizeToken(fifaCode);
        return new PublicTeamVisual(
                teamId,
                mapper.sanitizeText(teamName),
                sanitizedFifaCode,
                mapper.sanitizeToken(countryIso2),
                mapper.sanitizeText(flagAssetKey),
                mapper.sanitizeText(countryRegion)
        );
    }

    private String teamName(ResultSet rs, String joinedColumn, String payloadFieldName, String fallback) throws SQLException {
        return mapper.publicTeamName(rs.getString(joinedColumn), rs.getString("match_raw_payload"), payloadFieldName, fallback);
    }

    private PublicScoreboard scoreboard(Integer homeScore, Integer awayScore, Integer homeEventScore, Integer awayEventScore, String evidenceSummary,
                                        String status, String resultStatus, LocalDateTime kickoffTime) {
        String scoreSource = null;
        Integer resolvedHome = homeScore;
        Integer resolvedAway = awayScore;
        if (resolvedHome != null && resolvedAway != null) {
            scoreSource = "TEAM_STATS";
        } else if (homeEventScore != null && awayEventScore != null && homeEventScore + awayEventScore > 0) {
            resolvedHome = homeEventScore;
            resolvedAway = awayEventScore;
            scoreSource = "MATCH_EVENTS";
        } else {
            int[] parsed = parseScore(evidenceSummary);
            if (parsed != null) {
                resolvedHome = parsed[0];
                resolvedAway = parsed[1];
                scoreSource = "EVIDENCE_TEXT";
            }
        }

        if (resolvedHome != null && resolvedAway != null) {
            String winnerSide;
            String resultText;
            if (resolvedHome > resolvedAway) {
                winnerSide = "HOME";
                resultText = "主队胜";
            } else if (resolvedAway > resolvedHome) {
                winnerSide = "AWAY";
                resultText = "客队胜";
            } else {
                winnerSide = "DRAW";
                resultText = "平局";
            }
            return new PublicScoreboard(resolvedHome, resolvedAway, resolvedHome + " - " + resolvedAway,
                    winnerSide, resultText, scoreSource);
        }

        String normalizedStatus = status == null ? "" : status.toUpperCase(java.util.Locale.ROOT);
        String normalizedResultStatus = resultStatus == null ? "" : resultStatus.toUpperCase(java.util.Locale.ROOT);
        boolean finished = normalizedStatus.contains("FINISHED")
                || normalizedResultStatus.contains("FINAL")
                || normalizedResultStatus.contains("FINISHED");
        if (finished) {
            return new PublicScoreboard(null, null, "比分待核对", "UNKNOWN", "完赛 · 比分待核对", null);
        }
        if (kickoffTime != null) {
            return new PublicScoreboard(null, null, "待开球", "UNKNOWN", "未开赛", null);
        }
        return new PublicScoreboard(null, null, "待同步", "UNKNOWN", "赛程待同步", null);
    }

    private int[] parseScore(String evidenceSummary) {
        if (evidenceSummary == null || evidenceSummary.isBlank()) {
            return null;
        }
        Matcher matcher = SCORE_PATTERN.matcher(evidenceSummary);
        if (!matcher.find()) {
            return null;
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private record TeamRow(
            Long teamId,
            String teamKey,
            String teamName,
            String fifaCode,
            String countryRegion,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment
    ) {
    }

    private record PlayerRow(
            Long playerId,
            String playerKey,
            Long teamId,
            String teamName,
            String playerName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus
    ) {
    }

    private record OddsMarketRow(
            Long marketId,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            String sourceRef
    ) {
    }

    private record SentimentFactorRow(
            Long factorId,
            Long matchId,
            String factorCategory,
            String factorType,
            String title,
            String summary,
            String impactDirection,
            String entityType,
            String entityKey,
            String evidenceLevel,
            String sourceName,
            String sourceUrl,
            String sourceRef,
            LocalDateTime observedAt,
            LocalDateTime expiresAt,
            BigDecimal confidenceScore,
            BigDecimal reliabilityScore
    ) {
    }

    private record SummaryRow(
            PublicPrematchMatchSummary summary,
            long teamProfileCount,
            long playerProfileCount,
            long lineupCount,
            long oddsMarketCount,
            long staleLiveOddsCount,
            long sentimentFactorCount,
            long staleSentimentCount,
            long analysisReportCount,
            long evidenceCount,
            long unresolvedConflictCount
    ) {
    }
}
