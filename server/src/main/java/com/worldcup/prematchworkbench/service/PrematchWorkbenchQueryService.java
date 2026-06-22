package com.worldcup.prematchworkbench.service;

import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.IntegrityCheckResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.PrematchWorkbenchDetailResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchAnalysisReportResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchBetPlanItemResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchBetPlanResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchBetRecordResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchConflictResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchEvidenceResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchLineupResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchMatchSummaryResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchOddsMarketResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchOddsSelectionResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchPlayerFactResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchPlayerResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchSentimentFactorResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchSentimentRiskResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchTeamFactResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchTeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrematchWorkbenchQueryService {
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_MISSING = "MISSING";
    private static final String STATUS_STALE = "STALE";
    private static final String STATUS_CONFLICT = "CONFLICT";

    private final JdbcTemplate jdbcTemplate;

    public PrematchWorkbenchQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<WorkbenchMatchSummaryResponse> matches() {
        return jdbcTemplate.query(matchSelect() + " ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC", matchRowMapper())
                .stream()
                .map(row -> toSummary(row, integrityChecksFor(row)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrematchWorkbenchDetailResponse match(long matchId) {
        MatchRow row = findMatchRow(matchId);
        List<IntegrityCheckResponse> checks = integrityChecksFor(row);
        return new PrematchWorkbenchDetailResponse(
                toSummary(row, checks),
                teams(matchId),
                lineups(matchId),
                players(matchId),
                oddsMarkets(matchId),
                sentimentFactors(matchId),
                evidence(matchId),
                conflicts(matchId),
                analysisReports(matchId),
                betPlans(matchId),
                bets(matchId),
                checks
        );
    }

    @Transactional(readOnly = true)
    public List<IntegrityCheckResponse> integrity(long matchId) {
        return integrityChecks(matchId);
    }

    @Transactional(readOnly = true)
    public List<IntegrityCheckResponse> integrityChecks(long matchId) {
        return integrityChecksFor(findMatchRow(matchId));
    }

    private WorkbenchMatchSummaryResponse toSummary(MatchRow row, List<IntegrityCheckResponse> checks) {
        long passCount = checks.stream().filter(check -> STATUS_PASS.equals(check.status())).count();
        int score = checks.isEmpty() ? 0 : (int) Math.round(passCount * 100.0 / checks.size());
        return new WorkbenchMatchSummaryResponse(
                row.matchId(),
                row.matchKey(),
                row.matchName(),
                row.matchday(),
                row.jcCode(),
                row.competition(),
                row.stage(),
                row.venue(),
                row.kickoffTime(),
                row.status(),
                row.resultStatus(),
                row.homeTeamId(),
                row.homeTeamName(),
                row.awayTeamId(),
                row.awayTeamName(),
                score,
                checks.stream().filter(check -> STATUS_MISSING.equals(check.status())).count(),
                checks.stream().filter(check -> STATUS_STALE.equals(check.status())).count(),
                checks.stream().filter(check -> STATUS_CONFLICT.equals(check.status())).count(),
                teamProfileCount(row),
                playerProfileCount(row.matchId()),
                count("SELECT COUNT(*) FROM match_lineups WHERE match_id=?", row.matchId()),
                count("SELECT COUNT(*) FROM odds_market_snapshots WHERE match_id=?", row.matchId()),
                count("SELECT COUNT(*) FROM match_context_factors WHERE match_id=?", row.matchId()),
                count("SELECT COUNT(*) FROM analysis_reports WHERE match_id=?", row.matchId()),
                count("SELECT COUNT(*) FROM bet_plans WHERE match_id=?", row.matchId()),
                count("SELECT COUNT(*) FROM bets WHERE match_id=?", row.matchId())
        );
    }

    private List<IntegrityCheckResponse> integrityChecksFor(MatchRow row) {
        List<IntegrityCheckResponse> checks = new ArrayList<>();
        checks.add(teamProfileCheck(row));
        checks.add(playerProfileCheck(row.matchId()));
        checks.add(lineupCheck(row.matchId()));
        checks.add(oddsMarketCheck(row.matchId()));
        checks.add(liveOddsFreshnessCheck(row.matchId()));
        checks.add(sentimentFactorCheck(row.matchId()));
        checks.add(analysisReportCheck(row.matchId()));
        checks.add(aiBetPlanCheck(row.matchId()));
        checks.add(multiSourceEvidenceCheck(row.matchId()));
        checks.add(unresolvedConflictCheck(row.matchId()));
        return List.copyOf(checks);
    }

    private IntegrityCheckResponse teamProfileCheck(MatchRow row) {
        long homeCount = row.homeTeamId() == null ? 0 : count("SELECT COUNT(*) FROM team_profile_facts WHERE team_id=?", row.homeTeamId());
        long awayCount = row.awayTeamId() == null ? 0 : count("SELECT COUNT(*) FROM team_profile_facts WHERE team_id=?", row.awayTeamId());
        boolean pass = row.homeTeamId() != null && row.awayTeamId() != null && homeCount > 0 && awayCount > 0;
        return check(
                "TEAM_PROFILE",
                "球队画像",
                pass ? STATUS_PASS : STATUS_MISSING,
                pass ? "主客队球队画像已准备" : "主队或客队缺少球队画像事实",
                homeCount + awayCount,
                max(latestTimestamp("SELECT MAX(updated_at) FROM team_profile_facts WHERE team_id=?", row.homeTeamId()),
                        latestTimestamp("SELECT MAX(updated_at) FROM team_profile_facts WHERE team_id=?", row.awayTeamId()))
        );
    }

    private IntegrityCheckResponse playerProfileCheck(long matchId) {
        long lineupPlayers = count("SELECT COUNT(DISTINCT player_id) FROM match_lineups WHERE match_id=? AND player_id IS NOT NULL", matchId);
        long profiledPlayers = count("""
                SELECT COUNT(DISTINCT l.player_id)
                FROM match_lineups l
                WHERE l.match_id=? AND l.player_id IS NOT NULL
                  AND EXISTS (SELECT 1 FROM player_profile_facts f WHERE f.player_id=l.player_id)
                """, matchId);
        boolean pass = lineupPlayers > 0 && profiledPlayers == lineupPlayers;
        return check(
                "PLAYER_PROFILE",
                "球员画像",
                pass ? STATUS_PASS : STATUS_MISSING,
                pass ? "上阵球员画像已覆盖" : "阵容球员缺少画像或尚未录入阵容",
                profiledPlayers,
                latestTimestamp("""
                        SELECT MAX(f.updated_at)
                        FROM player_profile_facts f
                        WHERE EXISTS (
                            SELECT 1 FROM match_lineups l
                            WHERE l.match_id=? AND l.player_id=f.player_id
                        )
                        """, matchId)
        );
    }

    private IntegrityCheckResponse lineupCheck(long matchId) {
        long lineupCount = count("SELECT COUNT(*) FROM match_lineups WHERE match_id=?", matchId);
        long starterCount = count("SELECT COUNT(*) FROM match_lineups WHERE match_id=? AND is_starter=TRUE", matchId);
        boolean pass = lineupCount > 0 && starterCount > 0;
        return check(
                "LINEUP",
                "首发阵容",
                pass ? STATUS_PASS : STATUS_MISSING,
                pass ? "阵容与首发信息已录入" : "缺少阵容或首发信息",
                lineupCount,
                latestTimestamp("SELECT MAX(created_at) FROM match_lineups WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse oddsMarketCheck(long matchId) {
        long marketCount = count("SELECT COUNT(*) FROM odds_market_snapshots WHERE match_id=?", matchId);
        boolean pass = marketCount > 0;
        return check(
                "ODDS_MARKET",
                "赔率盘口",
                pass ? STATUS_PASS : STATUS_MISSING,
                pass ? "盘口快照已入库" : "缺少赔率盘口快照",
                marketCount,
                latestTimestamp("SELECT MAX(captured_at) FROM odds_market_snapshots WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse liveOddsFreshnessCheck(long matchId) {
        long liveCount = count("SELECT COUNT(*) FROM odds_market_snapshots WHERE match_id=? AND UPPER(snapshot_type)='LIVE'", matchId);
        LocalDateTime latest = latestTimestamp("SELECT MAX(captured_at) FROM odds_market_snapshots WHERE match_id=? AND UPPER(snapshot_type)='LIVE'", matchId);
        if (liveCount == 0) {
            return check("LIVE_ODDS_FRESHNESS", "临场赔率时效", STATUS_MISSING, "缺少 LIVE 临场赔率快照", 0, latest);
        }
        if (latest == null || latest.isBefore(LocalDateTime.now().minusHours(3))) {
            return check("LIVE_ODDS_FRESHNESS", "临场赔率时效", STATUS_STALE, "最新 LIVE 赔率距当前超过 3 小时或缺少采集时间", liveCount, latest);
        }
        return check("LIVE_ODDS_FRESHNESS", "临场赔率时效", STATUS_PASS, "LIVE 赔率在 3 小时内更新", liveCount, latest);
    }

    private IntegrityCheckResponse sentimentFactorCheck(long matchId) {
        long factorCount = count("SELECT COUNT(*) FROM match_context_factors WHERE match_id=?", matchId);
        LocalDateTime latest = latestTimestamp("SELECT MAX(updated_at) FROM match_context_factors WHERE match_id=?", matchId);
        if (factorCount == 0) {
            return check("SENTIMENT_FACTOR", "舆情外部因素", STATUS_MISSING, "缺少舆情或外部因素记录", 0, latest);
        }
        long expiredCount = count("SELECT COUNT(*) FROM match_context_factors WHERE match_id=? AND expires_at IS NOT NULL AND expires_at < ?", matchId, LocalDateTime.now());
        if (expiredCount > 0) {
            return check("SENTIMENT_FACTOR", "舆情外部因素", STATUS_STALE, "存在已过期舆情或外部因素", factorCount, latest);
        }
        return check("SENTIMENT_FACTOR", "舆情外部因素", STATUS_PASS, "舆情与外部因素未过期", factorCount, latest);
    }

    private IntegrityCheckResponse analysisReportCheck(long matchId) {
        long reportCount = count("SELECT COUNT(*) FROM analysis_reports WHERE match_id=?", matchId);
        return check(
                "ANALYSIS_REPORT",
                "分析报告",
                reportCount > 0 ? STATUS_PASS : STATUS_MISSING,
                reportCount > 0 ? "AI/人工分析报告已归档" : "缺少分析报告",
                reportCount,
                latestTimestamp("SELECT MAX(updated_at) FROM analysis_reports WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse aiBetPlanCheck(long matchId) {
        long planCount = count("SELECT COUNT(*) FROM bet_plans WHERE match_id=?", matchId);
        return check(
                "AI_BET_PLAN",
                "AI 下注方案",
                planCount > 0 ? STATUS_PASS : STATUS_MISSING,
                planCount > 0 ? "AI 下注方案 JSON 已入库" : "缺少 AI 下注方案",
                planCount,
                latestTimestamp("SELECT MAX(updated_at) FROM bet_plans WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse multiSourceEvidenceCheck(long matchId) {
        long evidenceCount = count("SELECT COUNT(*) FROM source_evidence WHERE match_id=?", matchId);
        return check(
                "MULTI_SOURCE_EVIDENCE",
                "多源证据",
                evidenceCount >= 2 ? STATUS_PASS : STATUS_MISSING,
                evidenceCount >= 2 ? "已满足至少 2 个独立来源" : "独立来源不足 2 个",
                evidenceCount,
                latestTimestamp("SELECT MAX(evidence_time) FROM source_evidence WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse unresolvedConflictCheck(long matchId) {
        long unresolved = count("SELECT COUNT(*) FROM data_conflicts WHERE match_id=? AND (resolution_status IS NULL OR resolution_status <> 'RESOLVED')", matchId);
        long total = count("SELECT COUNT(*) FROM data_conflicts WHERE match_id=?", matchId);
        return check(
                "UNRESOLVED_CONFLICT",
                "未解决冲突",
                unresolved > 0 ? STATUS_CONFLICT : STATUS_PASS,
                unresolved > 0 ? "存在未解决数据冲突，需要人工确认" : "无未解决数据冲突",
                total,
                latestTimestamp("SELECT MAX(created_at) FROM data_conflicts WHERE match_id=?", matchId)
        );
    }

    private IntegrityCheckResponse check(String code, String label, String status, String message, long evidenceCount, LocalDateTime lastUpdatedAt) {
        String severity = switch (status) {
            case STATUS_PASS -> "INFO";
            case STATUS_STALE -> "MEDIUM";
            case STATUS_CONFLICT -> "HIGH";
            default -> "HIGH";
        };
        return new IntegrityCheckResponse(code, label, status, severity, message, evidenceCount, lastUpdatedAt);
    }

    private List<WorkbenchTeamResponse> teams(long matchId) {
        return jdbcTemplate.query("""
                SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags,
                       t.attack_profile, t.defense_profile, t.public_sentiment
                FROM matches m
                JOIN teams t ON t.id IN (m.home_team_id, m.away_team_id)
                WHERE m.id=?
                ORDER BY CASE WHEN t.id=m.home_team_id THEN 0 ELSE 1 END, t.display_name
                """, (rs, rowNum) -> {
            long teamId = rs.getLong("id");
            return new WorkbenchTeamResponse(
                    teamId,
                    rs.getString("team_key"),
                    rs.getString("display_name"),
                    rs.getString("fifa_code"),
                    rs.getString("country_region"),
                    rs.getString("style_tags"),
                    rs.getString("attack_profile"),
                    rs.getString("defense_profile"),
                    rs.getString("public_sentiment"),
                    teamFacts(teamId)
            );
        }, matchId);
    }

    private List<WorkbenchTeamFactResponse> teamFacts(long teamId) {
        return jdbcTemplate.query("""
                SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM team_profile_facts
                WHERE team_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> new WorkbenchTeamFactResponse(
                rs.getLong("id"),
                rs.getString("fact_type"),
                rs.getString("period_key"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("sentiment_label"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("source_ref"),
                localDateTime(rs, "captured_at")
        ), teamId);
    }

    private List<WorkbenchLineupResponse> lineups(long matchId) {
        return jdbcTemplate.query("""
                SELECT l.id, l.match_id, l.team_id, COALESCE(t.display_name, '未知球队') AS team_name,
                       l.player_id, COALESCE(p.display_name, '未知球员') AS player_name, l.role, l.position, l.is_starter
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                LEFT JOIN teams t ON t.id=l.team_id
                LEFT JOIN players p ON p.id=l.player_id
                WHERE l.match_id=?
                ORDER BY CASE WHEN l.team_id=m.home_team_id THEN 0 ELSE 1 END, l.is_starter DESC, p.shirt_number, p.display_name, l.id
                """, lineupMapper(), matchId);
    }

    private List<WorkbenchPlayerResponse> players(long matchId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.player_key, p.team_id, COALESCE(t.display_name, '未知球队') AS team_name, p.display_name,
                       p.shirt_number, p.position, p.status, p.injury_status, p.card_status, p.locker_room_status
                FROM players p
                JOIN matches m ON m.id=?
                LEFT JOIN teams t ON t.id=p.team_id
                WHERE EXISTS (SELECT 1 FROM match_lineups l WHERE l.match_id=m.id AND l.player_id=p.id)
                ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, p.id
                """, (rs, rowNum) -> {
            long playerId = rs.getLong("id");
            return new WorkbenchPlayerResponse(
                    playerId,
                    rs.getString("player_key"),
                    nullableLong(rs, "team_id"),
                    rs.getString("team_name"),
                    rs.getString("display_name"),
                    nullableInt(rs, "shirt_number"),
                    rs.getString("position"),
                    rs.getString("status"),
                    rs.getString("injury_status"),
                    rs.getString("card_status"),
                    rs.getString("locker_room_status"),
                    playerFacts(playerId)
            );
        }, matchId);
    }

    private List<WorkbenchPlayerFactResponse> playerFacts(long playerId) {
        return jdbcTemplate.query("""
                SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM player_profile_facts
                WHERE player_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> new WorkbenchPlayerFactResponse(
                rs.getLong("id"),
                rs.getString("fact_type"),
                rs.getString("period_key"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("sentiment_label"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("source_ref"),
                localDateTime(rs, "captured_at")
        ), playerId);
    }

    private List<WorkbenchOddsMarketResponse> oddsMarkets(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref
                FROM odds_market_snapshots
                WHERE match_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> {
            long marketId = rs.getLong("id");
            return new WorkbenchOddsMarketResponse(
                    marketId,
                    rs.getString("bookmaker"),
                    rs.getString("market_code"),
                    rs.getString("market_name"),
                    rs.getString("snapshot_type"),
                    rs.getBigDecimal("handicap_line"),
                    rs.getString("line_value"),
                    localDateTime(rs, "captured_at"),
                    rs.getString("source_ref"),
                    oddsSelections(marketId)
            );
        }, matchId);
    }

    private List<WorkbenchOddsSelectionResponse> oddsSelections(long marketId) {
        return jdbcTemplate.query("""
                SELECT id, selection_code, selection_name, odds_value, implied_probability, selection_status
                FROM odds_selection_snapshots
                WHERE market_snapshot_id=?
                ORDER BY id
                """, (rs, rowNum) -> new WorkbenchOddsSelectionResponse(
                rs.getLong("id"),
                rs.getString("selection_code"),
                rs.getString("selection_name"),
                rs.getBigDecimal("odds_value"),
                rs.getBigDecimal("implied_probability"),
                rs.getString("selection_status")
        ), marketId);
    }

    private List<WorkbenchSentimentFactorResponse> sentimentFactors(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key,
                       evidence_level, source_name, source_url, source_ref, observed_at, expires_at, confidence_score, reliability_score
                FROM match_context_factors
                WHERE match_id=?
                ORDER BY observed_at DESC, id DESC
                """, (rs, rowNum) -> {
            long factorId = rs.getLong("id");
            return new WorkbenchSentimentFactorResponse(
                    factorId,
                    nullableLong(rs, "match_id"),
                    rs.getString("factor_category"),
                    rs.getString("factor_type"),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("impact_direction"),
                    rs.getString("entity_type"),
                    rs.getString("entity_key"),
                    rs.getString("evidence_level"),
                    rs.getString("source_name"),
                    rs.getString("source_url"),
                    rs.getString("source_ref"),
                    localDateTime(rs, "observed_at"),
                    localDateTime(rs, "expires_at"),
                    rs.getBigDecimal("confidence_score"),
                    rs.getBigDecimal("reliability_score"),
                    sentimentRisks(factorId)
            );
        }, matchId);
    }

    private List<WorkbenchSentimentRiskResponse> sentimentRisks(long factorId) {
        return jdbcTemplate.query("""
                SELECT id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref
                FROM sentiment_risk_assessments
                WHERE factor_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new WorkbenchSentimentRiskResponse(
                rs.getLong("id"),
                rs.getString("risk_type"),
                rs.getString("risk_level"),
                rs.getBigDecimal("risk_score"),
                rs.getString("title"),
                rs.getString("rationale"),
                rs.getString("suggested_action"),
                rs.getString("source_name"),
                rs.getString("source_ref")
        ), factorId);
    }

    private List<WorkbenchEvidenceResponse> evidence(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score
                FROM source_evidence
                WHERE match_id=?
                ORDER BY evidence_time DESC, id
                """, (rs, rowNum) -> new WorkbenchEvidenceResponse(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getString("source_name"),
                rs.getString("source_ref"),
                rs.getString("source_url"),
                localDateTime(rs, "evidence_time"),
                rs.getString("summary"),
                rs.getBigDecimal("reliability_score")
        ), matchId);
    }

    private List<WorkbenchConflictResponse> conflicts(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload
                FROM data_conflicts
                WHERE match_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new WorkbenchConflictResponse(
                rs.getLong("id"),
                rs.getString("conflict_type"),
                rs.getString("entity_key"),
                rs.getString("field_name"),
                rs.getString("current_value"),
                rs.getString("incoming_value"),
                rs.getString("resolution_status"),
                rs.getString("raw_payload")
        ), matchId);
    }

    private List<WorkbenchAnalysisReportResponse> analysisReports(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions,
                       narrative_md, created_at, updated_at
                FROM analysis_reports
                WHERE match_id=?
                ORDER BY created_at DESC, id DESC
                """, (rs, rowNum) -> new WorkbenchAnalysisReportResponse(
                rs.getLong("id"),
                rs.getString("analysis_id"),
                rs.getString("conclusion_type"),
                rs.getString("confidence"),
                rs.getString("risk_summary"),
                rs.getString("recommended_markets"),
                rs.getString("dimensions"),
                rs.getString("narrative_md"),
                localDateTime(rs, "created_at"),
                localDateTime(rs, "updated_at")
        ), matchId);
    }

    private List<WorkbenchBetPlanResponse> betPlans(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, analysis_report_id, plan_key, plan_title, conclusion_type, confidence, budget_amount, risk_summary,
                       betting_method, strategy_type, status, generated_by, generated_at
                FROM bet_plans
                WHERE match_id=?
                ORDER BY generated_at DESC, id DESC
                """, (rs, rowNum) -> {
            long planId = rs.getLong("id");
            return new WorkbenchBetPlanResponse(
                    planId,
                    nullableLong(rs, "analysis_report_id"),
                    rs.getString("plan_key"),
                    rs.getString("plan_title"),
                    rs.getString("conclusion_type"),
                    rs.getString("confidence"),
                    rs.getBigDecimal("budget_amount"),
                    rs.getString("risk_summary"),
                    rs.getString("betting_method"),
                    rs.getString("strategy_type"),
                    rs.getString("status"),
                    rs.getString("generated_by"),
                    localDateTime(rs, "generated_at"),
                    betPlanItems(planId)
            );
        }, matchId);
    }

    private List<WorkbenchBetPlanItemResponse> betPlanItems(long planId) {
        return jdbcTemplate.query("""
                SELECT id, match_id, market_type, selection_text, stake_suggestion, odds, line_value, logic_type,
                       risk_level, play_type, pass_type, item_order
                FROM bet_plan_items
                WHERE bet_plan_id=?
                ORDER BY item_order, id
                """, (rs, rowNum) -> new WorkbenchBetPlanItemResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                rs.getString("market_type"),
                rs.getString("selection_text"),
                rs.getBigDecimal("stake_suggestion"),
                rs.getBigDecimal("odds"),
                rs.getString("line_value"),
                rs.getString("logic_type"),
                rs.getString("risk_level"),
                rs.getString("play_type"),
                rs.getString("pass_type"),
                rs.getInt("item_order")
        ), planId);
    }

    private List<WorkbenchBetRecordResponse> bets(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, bet_id, ticket_no, bet_date, matchday, match_name, market_type, selection_text, stake, odds,
                       closing_odds, clv, return_amount, hit_status, profit_loss, review_status
                FROM bets
                WHERE match_id=?
                ORDER BY bet_date DESC, id DESC
                """, (rs, rowNum) -> new WorkbenchBetRecordResponse(
                rs.getLong("id"),
                rs.getString("bet_id"),
                rs.getString("ticket_no"),
                localDate(rs, "bet_date"),
                localDate(rs, "matchday"),
                rs.getString("match_name"),
                rs.getString("market_type"),
                rs.getString("selection_text"),
                rs.getBigDecimal("stake"),
                rs.getBigDecimal("odds"),
                rs.getBigDecimal("closing_odds"),
                rs.getBigDecimal("clv"),
                rs.getBigDecimal("return_amount"),
                rs.getString("hit_status"),
                rs.getBigDecimal("profit_loss"),
                rs.getString("review_status")
        ), matchId);
    }

    private MatchRow findMatchRow(long matchId) {
        return jdbcTemplate.query(matchSelect() + " WHERE m.id=?", matchRowMapper(), matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "比赛不存在"));
    }

    private String matchSelect() {
        return """
                SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue,
                       m.kickoff_time, m.status, m.result_status, m.home_team_id,
                       COALESCE(ht.display_name, '未知主队') AS home_team_name,
                       m.away_team_id, COALESCE(at.display_name, '未知客队') AS away_team_name
                FROM matches m
                LEFT JOIN teams ht ON ht.id=m.home_team_id
                LEFT JOIN teams at ON at.id=m.away_team_id
                """;
    }

    private RowMapper<MatchRow> matchRowMapper() {
        return (rs, rowNum) -> new MatchRow(
                rs.getLong("id"),
                rs.getString("match_key"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("competition"),
                rs.getString("stage"),
                rs.getString("venue"),
                localDateTime(rs, "kickoff_time"),
                rs.getString("status"),
                rs.getString("result_status"),
                nullableLong(rs, "home_team_id"),
                rs.getString("home_team_name"),
                nullableLong(rs, "away_team_id"),
                rs.getString("away_team_name")
        );
    }

    private RowMapper<WorkbenchLineupResponse> lineupMapper() {
        return (rs, rowNum) -> new WorkbenchLineupResponse(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                rs.getString("team_name"),
                nullableLong(rs, "player_id"),
                rs.getString("player_name"),
                rs.getString("role"),
                rs.getString("position"),
                rs.getBoolean("is_starter")
        );
    }

    private long teamProfileCount(MatchRow row) {
        return (row.homeTeamId() == null ? 0 : count("SELECT COUNT(*) FROM team_profile_facts WHERE team_id=?", row.homeTeamId()))
                + (row.awayTeamId() == null ? 0 : count("SELECT COUNT(*) FROM team_profile_facts WHERE team_id=?", row.awayTeamId()));
    }

    private long playerProfileCount(long matchId) {
        return count("""
                SELECT COUNT(*)
                FROM player_profile_facts f
                WHERE EXISTS (
                    SELECT 1 FROM match_lineups l
                    WHERE l.match_id=? AND l.player_id=f.player_id
                )
                """, matchId);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private LocalDateTime latestTimestamp(String sql, Object... args) {
        for (Object arg : args) {
            if (arg == null) {
                return null;
            }
        }
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return localDateTime(rs, 1);
        }, args);
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDateTime localDateTime(ResultSet rs, int column) throws SQLException {
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

    private record MatchRow(
            Long matchId,
            String matchKey,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String competition,
            String stage,
            String venue,
            LocalDateTime kickoffTime,
            String status,
            String resultStatus,
            Long homeTeamId,
            String homeTeamName,
            Long awayTeamId,
            String awayTeamName
    ) {
    }
}
