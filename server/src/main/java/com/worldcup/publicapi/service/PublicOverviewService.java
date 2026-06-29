package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicIntegrityCounters;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsFreshness;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOverviewMatch;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOverviewResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicRiskCounters;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicScoreboard;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamVisual;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicOverviewService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:比分\\s*[=：:]?\\s*)?(\\d{1,2})\\s*[-:：比]\\s*(\\d{1,2})");

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;
    private final Clock clock;

    public PublicOverviewService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PublicOverviewResponse overview() {
        return new PublicOverviewResponse(
                LocalDateTime.now(clock),
                upcomingMatches(),
                riskCounters(),
                integrityCounters(),
                oddsFreshness(),
                decisionSummary()
        );
    }

    private List<PublicOverviewMatch> upcomingMatches() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        List<PublicOverviewMatch> todayMatches = overviewMatches("""
                WHERE (m.kickoff_time >= ? AND m.kickoff_time < ?)
                   OR (m.kickoff_time IS NULL AND m.matchday = ?)
                """,
                "ORDER BY CASE WHEN m.kickoff_time IS NULL THEN 1 ELSE 0 END, m.kickoff_time ASC, m.id ASC",
                java.sql.Timestamp.valueOf(now),
                java.sql.Timestamp.valueOf(tomorrowStart),
                java.sql.Date.valueOf(today));
        if (!todayMatches.isEmpty()) {
            return todayMatches;
        }

        List<PublicOverviewMatch> futureMatches = overviewMatches("""
                WHERE m.kickoff_time >= ?
                   OR (m.kickoff_time IS NULL AND m.matchday > ?)
                """,
                "ORDER BY CASE WHEN m.kickoff_time IS NULL THEN 1 ELSE 0 END, m.kickoff_time ASC, m.id ASC",
                java.sql.Timestamp.valueOf(now),
                java.sql.Date.valueOf(today));
        if (!futureMatches.isEmpty()) {
            return futureMatches;
        }

        return overviewMatches("""
                WHERE m.kickoff_time < ?
                   OR (m.kickoff_time IS NULL AND m.matchday <= ?)
                """,
                """
                ORDER BY CASE
                           WHEN EXISTS (SELECT 1 FROM match_team_stats s WHERE s.match_id=m.id)
                             OR EXISTS (SELECT 1 FROM match_events e
                                        WHERE e.match_id=m.id
                                          AND (UPPER(e.event_type) = 'GOAL'
                                               OR UPPER(e.event_type) LIKE 'GOAL_%%'
                                               OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED')))
                             OR UPPER(COALESCE(m.result_status, '')) IN ('FINAL', 'FINISHED')
                             OR UPPER(COALESCE(m.status, '')) IN ('FINAL', 'FINISHED')
                           THEN 0 ELSE 1
                         END,
                         m.matchday DESC,
                         CASE WHEN m.kickoff_time IS NULL THEN 1 ELSE 0 END,
                         m.kickoff_time DESC,
                         m.id DESC
                """,
                java.sql.Timestamp.valueOf(now),
                java.sql.Date.valueOf(today));
    }

    private List<PublicOverviewMatch> overviewMatches(String whereClause, String orderClause, Object... args) {
        return jdbcTemplate.query("""
                SELECT m.id, m.match_name, m.matchday, m.jc_code, m.competition, m.stage,
                       m.kickoff_time, m.status, m.result_status, m.raw_payload AS match_raw_payload,
                       m.home_team_id,
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
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score,
                       (SELECT COUNT(*) FROM match_events e
                        WHERE e.match_id=m.id AND e.team_id=m.away_team_id
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score,
                       (SELECT ev.summary FROM source_evidence ev
                        WHERE ev.match_id=m.id AND ev.summary IS NOT NULL
                        ORDER BY ev.reliability_score DESC, ev.id DESC LIMIT 1) AS score_evidence_summary,
                       (SELECT COUNT(*) FROM sentiment_risk_assessments sra
                        WHERE sra.match_id=m.id AND UPPER(sra.risk_level) IN ('HIGH', 'CRITICAL')) AS risk_count,
                       CASE
                           WHEN EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                            AND EXISTS (SELECT 1 FROM odds_market_snapshots oms WHERE oms.match_id=m.id)
                            AND EXISTS (SELECT 1 FROM match_lineups ml WHERE ml.match_id=m.id)
                           THEN 100 ELSE 0
                       END AS integrity_score
                FROM matches m
                LEFT JOIN teams ht ON ht.id=m.home_team_id
                LEFT JOIN teams at ON at.id=m.away_team_id
                %s
                %s
                LIMIT 5
                """.formatted(whereClause, orderClause), (rs, rowNum) -> new PublicOverviewMatch(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeText(rs.getString("competition")),
                mapper.sanitizeText(rs.getString("stage")),
                localDateTime(rs, "kickoff_time"),
                mapper.sanitizeToken(rs.getString("status")),
                mapper.sanitizeToken(rs.getString("result_status")),
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
                rs.getInt("integrity_score"),
                rs.getLong("risk_count")
        ), args);
    }

    private PublicRiskCounters riskCounters() {
        LocalDateTime now = LocalDateTime.now(clock);
        return new PublicRiskCounters(
                count("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE UPPER(risk_level) IN ('HIGH', 'CRITICAL')"),
                count("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE UPPER(risk_level) = 'MEDIUM'"),
                count("SELECT COUNT(*) FROM match_context_factors WHERE expires_at IS NOT NULL AND expires_at < ?", now),
                count("SELECT COUNT(*) FROM data_conflicts WHERE resolution_status IS NULL OR resolution_status <> 'RESOLVED'")
        );
    }

    private PublicIntegrityCounters integrityCounters() {
        long total = count("SELECT COUNT(*) FROM matches");
        long complete = count("""
                SELECT COUNT(*) FROM matches m
                WHERE EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                  AND EXISTS (SELECT 1 FROM odds_market_snapshots oms WHERE oms.match_id=m.id)
                  AND EXISTS (SELECT 1 FROM match_lineups ml WHERE ml.match_id=m.id)
                """);
        long blocked = count("""
                SELECT COUNT(*) FROM matches m
                WHERE NOT EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                """);
        long partial = Math.max(0, total - complete - blocked);
        return new PublicIntegrityCounters(complete, partial, blocked);
    }

    private PublicOddsFreshness oddsFreshness() {
        LocalDateTime staleCutoff = LocalDateTime.now(clock).minusHours(3);
        return new PublicOddsFreshness(
                count("SELECT COUNT(*) FROM odds_market_snapshots"),
                count("SELECT COUNT(*) FROM odds_market_snapshots WHERE UPPER(snapshot_type) = 'LIVE'"),
                count("SELECT COUNT(*) FROM odds_market_snapshots WHERE UPPER(snapshot_type) = 'LIVE' AND (captured_at IS NULL OR captured_at < ?)", staleCutoff)
        );
    }

    private PublicDecisionSummary decisionSummary() {
        LocalDateTime latestReport = latestTimestamp("SELECT MAX(updated_at) FROM analysis_reports");
        LocalDateTime latestReview = latestTimestamp("SELECT MAX(updated_at) FROM post_match_reviews");
        return new PublicDecisionSummary(
                count("SELECT COUNT(*) FROM analysis_reports"),
                count("SELECT COUNT(*) FROM post_match_reviews"),
                max(latestReport, latestReview)
        );
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private LocalDateTime latestTimestamp(String sql) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return localDateTime(rs, 1);
        });
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

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }
}
