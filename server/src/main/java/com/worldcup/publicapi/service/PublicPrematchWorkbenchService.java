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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class PublicPrematchWorkbenchService {
    private static final int DEFAULT_LIMIT = 50;

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicPrematchWorkbenchService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicPrematchMatchSummary> matches() {
        return jdbcTemplate.query(summarySelect() + """
                ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC
                LIMIT ?
                """, (rs, rowNum) -> summaryRow(rs).summary(), DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public PublicPrematchDetail match(long matchId) {
        SummaryRow summary = findSummary(matchId);
        return new PublicPrematchDetail(
                summary.summary(),
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
        return jdbcTemplate.query(summarySelect() + " WHERE m.id=?", (rs, rowNum) -> summaryRow(rs), matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private String summarySelect() {
        return """
                SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue,
                       m.kickoff_time, m.status, m.result_status, m.home_team_id,
                       COALESCE(ht.display_name, 'Unknown home team') AS home_team_name,
                       m.away_team_id, COALESCE(at.display_name, 'Unknown away team') AS away_team_name,
                       (SELECT COUNT(*) FROM team_profile_facts tf WHERE tf.team_id IN (m.home_team_id, m.away_team_id)) AS team_profile_count,
                       (SELECT COUNT(*)
                        FROM player_profile_facts pf
                        WHERE EXISTS (
                            SELECT 1 FROM match_lineups ml
                            WHERE ml.match_id=m.id AND ml.player_id=pf.player_id
                        )) AS player_profile_count,
                       (SELECT COUNT(*) FROM match_lineups ml WHERE ml.match_id=m.id) AS lineup_count,
                       (SELECT COUNT(*) FROM odds_market_snapshots oms WHERE oms.match_id=m.id) AS odds_market_count,
                       (SELECT COUNT(*) FROM odds_market_snapshots oms
                        WHERE oms.match_id=m.id AND UPPER(oms.snapshot_type)='LIVE'
                          AND (oms.captured_at IS NULL OR oms.captured_at < DATEADD('HOUR', -3, CURRENT_TIMESTAMP))) AS stale_live_odds_count,
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
                mapper.sanitizeText(rs.getString("home_team_name")),
                nullableLong(rs, "away_team_id"),
                mapper.sanitizeText(rs.getString("away_team_name")),
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

    private List<PublicPrematchTeam> teams(long matchId) {
        return jdbcTemplate.query("""
                SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags,
                       t.attack_profile, t.defense_profile, t.public_sentiment
                FROM matches m
                JOIN teams t ON t.id IN (m.home_team_id, m.away_team_id)
                WHERE m.id=?
                ORDER BY CASE WHEN t.id=m.home_team_id THEN 0 ELSE 1 END, t.display_name
                """, (rs, rowNum) -> {
            long teamId = rs.getLong("id");
            return new PublicPrematchTeam(
                    teamId,
                    mapper.sanitizeText(rs.getString("team_key")),
                    mapper.sanitizeText(rs.getString("display_name")),
                    mapper.sanitizeText(rs.getString("fifa_code")),
                    mapper.sanitizeText(rs.getString("country_region")),
                    mapper.sanitizeText(rs.getString("style_tags")),
                    mapper.sanitizeText(rs.getString("attack_profile")),
                    mapper.sanitizeText(rs.getString("defense_profile")),
                    mapper.sanitizeText(rs.getString("public_sentiment")),
                    teamFacts(teamId)
            );
        }, matchId);
    }

    private List<PublicPrematchFact> teamFacts(long teamId) {
        return jdbcTemplate.query("""
                SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM team_profile_facts
                WHERE team_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> fact(rs), teamId);
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
        return jdbcTemplate.query("""
                SELECT p.id, p.player_key, p.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name, p.display_name,
                       p.shirt_number, p.position, p.status, p.injury_status, p.card_status, p.locker_room_status
                FROM players p
                JOIN matches m ON m.id=?
                LEFT JOIN teams t ON t.id=p.team_id
                WHERE EXISTS (SELECT 1 FROM match_lineups l WHERE l.match_id=m.id AND l.player_id=p.id)
                ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, p.id
                """, (rs, rowNum) -> {
            long playerId = rs.getLong("id");
            return new PublicPrematchPlayer(
                    playerId,
                    mapper.sanitizeText(rs.getString("player_key")),
                    nullableLong(rs, "team_id"),
                    mapper.sanitizeText(rs.getString("team_name")),
                    mapper.sanitizeText(rs.getString("display_name")),
                    nullableInt(rs, "shirt_number"),
                    mapper.sanitizeText(rs.getString("position")),
                    mapper.sanitizeToken(rs.getString("status")),
                    mapper.sanitizeText(rs.getString("injury_status")),
                    mapper.sanitizeText(rs.getString("card_status")),
                    mapper.sanitizeText(rs.getString("locker_room_status")),
                    playerFacts(playerId)
            );
        }, matchId);
    }

    private List<PublicPrematchFact> playerFacts(long playerId) {
        return jdbcTemplate.query("""
                SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score,
                       source_name, source_url, source_ref, captured_at
                FROM player_profile_facts
                WHERE player_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> fact(rs), playerId);
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
        return jdbcTemplate.query("""
                SELECT id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref
                FROM odds_market_snapshots
                WHERE match_id=?
                ORDER BY captured_at DESC, id DESC
                """, (rs, rowNum) -> {
            long marketId = rs.getLong("id");
            return new PublicPrematchOddsMarket(
                    marketId,
                    mapper.sanitizeText(rs.getString("bookmaker")),
                    mapper.sanitizeToken(rs.getString("market_code")),
                    mapper.sanitizeText(rs.getString("market_name")),
                    mapper.sanitizeToken(rs.getString("snapshot_type")),
                    rs.getBigDecimal("handicap_line"),
                    mapper.sanitizeText(rs.getString("line_value")),
                    localDateTime(rs, "captured_at"),
                    mapper.sanitizeText(rs.getString("source_ref")),
                    oddsSelections(marketId)
            );
        }, matchId);
    }

    private List<PublicPrematchOddsSelection> oddsSelections(long marketId) {
        return jdbcTemplate.query("""
                SELECT id, selection_code, selection_name, odds_value, implied_probability, selection_status
                FROM odds_selection_snapshots
                WHERE market_snapshot_id=?
                ORDER BY id
                """, (rs, rowNum) -> new PublicPrematchOddsSelection(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("selection_code")),
                mapper.sanitizeText(rs.getString("selection_name")),
                rs.getBigDecimal("odds_value"),
                rs.getBigDecimal("implied_probability"),
                mapper.sanitizeToken(rs.getString("selection_status"))
        ), marketId);
    }

    private List<PublicPrematchSentimentFactor> sentimentFactors(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key,
                       evidence_level, source_name, source_url, source_ref, observed_at, expires_at, confidence_score, reliability_score
                FROM match_context_factors
                WHERE match_id=?
                ORDER BY observed_at DESC, id DESC
                """, (rs, rowNum) -> {
            long factorId = rs.getLong("id");
            return new PublicPrematchSentimentFactor(
                    factorId,
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
                    rs.getBigDecimal("reliability_score"),
                    sentimentRisks(factorId)
            );
        }, matchId);
    }

    private List<PublicPrematchSentimentRisk> sentimentRisks(long factorId) {
        return jdbcTemplate.query("""
                SELECT id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref
                FROM sentiment_risk_assessments
                WHERE factor_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new PublicPrematchSentimentRisk(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("risk_type")),
                mapper.sanitizeToken(rs.getString("risk_level")),
                rs.getBigDecimal("risk_score"),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("rationale")),
                mapper.sanitizeText(rs.getString("suggested_action")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_ref"))
        ), factorId);
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
