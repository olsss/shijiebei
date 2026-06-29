package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDictionary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsSelection;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicScoreboard;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamVisual;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicOddsService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:比分\\s*[=：:]?\\s*)?(\\d{1,2})\\s*[-:：比]\\s*(\\d{1,2})");

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicOddsService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicOddsMarketSummary> overview() {
        return jdbcTemplate.query(
                marketSummarySelect() + " ORDER BY m.matchday DESC, oms.captured_at DESC, oms.id DESC",
                (rs, rowNum) -> new PublicOddsMarketSummary(
                        rs.getLong("id"),
                        nullableLong(rs, "match_id"),
                        mapper.sanitizeText(rs.getString("match_name")),
                        localDate(rs, "matchday"),
                        mapper.sanitizeText(rs.getString("jc_code")),
                        teamVisual(rs, "home"),
                        teamVisual(rs, "away"),
                        scoreboard(
                                nullableLong(rs, "match_id"),
                                nullableInt(rs, "home_score"),
                                nullableInt(rs, "away_score"),
                                rs.getString("status"),
                                rs.getString("result_status")
                        ),
                        mapper.sanitizeText(rs.getString("bookmaker")),
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name")),
                        mapper.sanitizeToken(rs.getString("snapshot_type")),
                        rs.getBigDecimal("handicap_line"),
                        mapper.sanitizeText(rs.getString("line_value")),
                        localDateTime(rs, "captured_at"),
                        rs.getLong("selection_count")
                )
        );
    }

    @Transactional(readOnly = true)
    public PublicOddsMatchDetail matchOdds(long matchId) {
        MatchRow match = findMatch(matchId);
        List<MarketRow> marketRows = marketsForMatch(matchId);
        Map<Long, List<PublicOddsSelection>> selectionsByMarketId = selectionsByMarketIds(
                marketRows.stream().map(MarketRow::id).toList()
        );
        List<PublicOddsMarketDetail> markets = marketRows.stream()
                .map(row -> new PublicOddsMarketDetail(
                        row.id(),
                        row.matchId(),
                        row.matchName(),
                        row.matchday(),
                        row.jcCode(),
                        row.bookmaker(),
                        row.marketCode(),
                        row.marketName(),
                        row.snapshotType(),
                        row.handicapLine(),
                        row.lineValue(),
                        row.capturedAt(),
                        row.selectionCount(),
                        row.sourceRef(),
                        selectionsByMarketId.getOrDefault(row.id(), List.of())
                ))
                .toList();
        return new PublicOddsMatchDetail(
                match.id(),
                match.matchName(),
                match.matchday(),
                match.jcCode(),
                match.homeTeam(),
                match.awayTeam(),
                match.scoreboard(),
                markets
        );
    }

    @Transactional(readOnly = true)
    public List<String> bookmakers() {
        return jdbcTemplate.query(
                "SELECT DISTINCT bookmaker FROM odds_market_snapshots ORDER BY bookmaker",
                (rs, rowNum) -> mapper.sanitizeText(rs.getString("bookmaker"))
        );
    }

    @Transactional(readOnly = true)
    public List<PublicOddsMarketDictionary> markets() {
        return jdbcTemplate.query(
                "SELECT market_code, MIN(market_name) AS market_name FROM odds_market_snapshots GROUP BY market_code ORDER BY market_code",
                (rs, rowNum) -> new PublicOddsMarketDictionary(
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name"))
                )
        );
    }

    private MatchRow findMatch(long matchId) {
        return jdbcTemplate.query(
                        """
                        SELECT m.id, m.match_name, m.matchday, m.jc_code, m.status, m.result_status,
                               ht.id AS home_team_id, ht.display_name AS home_team_name, ht.fifa_code AS home_fifa_code,
                               ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region,
                               at.id AS away_team_id, at.display_name AS away_team_name, at.fifa_code AS away_fifa_code,
                               at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region,
                               (SELECT s.goals_for FROM match_team_stats s WHERE s.match_id=m.id AND s.team_id=m.home_team_id ORDER BY s.id DESC LIMIT 1) AS home_score,
                               (SELECT s.goals_for FROM match_team_stats s WHERE s.match_id=m.id AND s.team_id=m.away_team_id ORDER BY s.id DESC LIMIT 1) AS away_score
                        FROM matches m
                        LEFT JOIN teams ht ON ht.id=m.home_team_id
                        LEFT JOIN teams at ON at.id=m.away_team_id
                        WHERE m.id=?
                        """,
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                mapper.sanitizeText(rs.getString("match_name")),
                                localDate(rs, "matchday"),
                                mapper.sanitizeText(rs.getString("jc_code")),
                                teamVisual(rs, "home"),
                                teamVisual(rs, "away"),
                                scoreboard(
                                        rs.getLong("id"),
                                        nullableInt(rs, "home_score"),
                                        nullableInt(rs, "away_score"),
                                        rs.getString("status"),
                                        rs.getString("result_status")
                                )
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private List<MarketRow> marketsForMatch(long matchId) {
        return jdbcTemplate.query(
                marketDetailSelect() + " WHERE oms.match_id=? ORDER BY oms.bookmaker, oms.market_code, oms.captured_at DESC, oms.id DESC",
                (rs, rowNum) -> new MarketRow(
                        rs.getLong("id"),
                        nullableLong(rs, "match_id"),
                        mapper.sanitizeText(rs.getString("match_name")),
                        localDate(rs, "matchday"),
                        mapper.sanitizeText(rs.getString("jc_code")),
                        mapper.sanitizeText(rs.getString("bookmaker")),
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name")),
                        mapper.sanitizeToken(rs.getString("snapshot_type")),
                        rs.getBigDecimal("handicap_line"),
                        mapper.sanitizeText(rs.getString("line_value")),
                        localDateTime(rs, "captured_at"),
                        rs.getLong("selection_count"),
                        mapper.sanitizeText(rs.getString("source_ref"))
                ),
                matchId
        );
    }

    private Map<Long, List<PublicOddsSelection>> selectionsByMarketIds(List<Long> marketIds) {
        if (marketIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicOddsSelection>> grouped = new LinkedHashMap<>();
        String sql = "SELECT id, market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status "
                + "FROM odds_selection_snapshots WHERE market_snapshot_id IN (" + placeholders(marketIds.size()) + ") "
                + "ORDER BY market_snapshot_id, id";
        jdbcTemplate.query(sql, rs -> {
            long marketId = rs.getLong("market_snapshot_id");
            grouped.computeIfAbsent(marketId, ignored -> new ArrayList<>()).add(new PublicOddsSelection(
                    rs.getLong("id"),
                    marketId,
                    mapper.sanitizeToken(rs.getString("selection_code")),
                    mapper.sanitizeText(rs.getString("selection_name")),
                    rs.getBigDecimal("odds_value"),
                    rs.getBigDecimal("implied_probability"),
                    mapper.sanitizeToken(rs.getString("selection_status"))
            ));
        }, marketIds.toArray());
        return grouped;
    }

    private String marketSummarySelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, m.status, m.result_status, "
                + "ht.id AS home_team_id, ht.display_name AS home_team_name, ht.fifa_code AS home_fifa_code, "
                + "ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region, "
                + "at.id AS away_team_id, at.display_name AS away_team_name, at.fifa_code AS away_fifa_code, "
                + "at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region, "
                + "(SELECT s.goals_for FROM match_team_stats s WHERE s.match_id=m.id AND s.team_id=m.home_team_id ORDER BY s.id DESC LIMIT 1) AS home_score, "
                + "(SELECT s.goals_for FROM match_team_stats s WHERE s.match_id=m.id AND s.team_id=m.away_team_id ORDER BY s.id DESC LIMIT 1) AS away_score, "
                + "oms.bookmaker, oms.market_code, oms.market_name, oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, "
                + "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count "
                + "FROM odds_market_snapshots oms "
                + "LEFT JOIN matches m ON m.id=oms.match_id "
                + "LEFT JOIN teams ht ON ht.id=m.home_team_id "
                + "LEFT JOIN teams at ON at.id=m.away_team_id";
    }

    private String marketDetailSelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, oms.bookmaker, oms.market_code, oms.market_name, "
                + "oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, "
                + "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count, "
                + "oms.source_ref "
                + "FROM odds_market_snapshots oms LEFT JOIN matches m ON m.id=oms.match_id";
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private PublicTeamVisual teamVisual(ResultSet rs, String prefix) throws SQLException {
        return new PublicTeamVisual(
                nullableLong(rs, prefix + "_team_id"),
                mapper.sanitizeText(rs.getString(prefix + "_team_name")),
                mapper.sanitizeText(rs.getString(prefix + "_fifa_code")),
                mapper.sanitizeToken(rs.getString(prefix + "_country_iso2")),
                mapper.sanitizeText(rs.getString(prefix + "_flag_asset_key")),
                mapper.sanitizeText(rs.getString(prefix + "_country_region"))
        );
    }

    private PublicScoreboard scoreboard(Long matchId, Integer homeScore, Integer awayScore, String status, String resultStatus) {
        if (homeScore != null && awayScore != null) {
            return scoreboardFromNumbers(homeScore, awayScore, "TEAM_STATS");
        }
        PublicScoreboard eventScoreboard = eventScoreboard(matchId);
        if (eventScoreboard != null) {
            return eventScoreboard;
        }
        PublicScoreboard evidenceScoreboard = evidenceScoreboard(matchId);
        if (evidenceScoreboard != null) {
            return evidenceScoreboard;
        }
        String normalizedStatus = (status == null ? "" : status).toUpperCase();
        String normalizedResult = (resultStatus == null ? "" : resultStatus).toUpperCase();
        if (normalizedStatus.contains("FINISHED") || normalizedResult.contains("FINAL") || normalizedResult.contains("FINISHED")) {
            return new PublicScoreboard(null, null, "比分待核对", "UNKNOWN", "已完赛，等待比分核对", "PENDING");
        }
        return new PublicScoreboard(null, null, "待开球", "UNKNOWN", "赛前", "PENDING");
    }

    private PublicScoreboard eventScoreboard(Long matchId) {
        if (matchId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT
                  SUM(CASE WHEN e.team_id=m.home_team_id THEN 1 ELSE 0 END) AS home_event_score,
                  SUM(CASE WHEN e.team_id=m.away_team_id THEN 1 ELSE 0 END) AS away_event_score
                FROM matches m
                LEFT JOIN match_events e ON e.match_id=m.id
                  AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))
                WHERE m.id=?
                """, rs -> {
            if (!rs.next()) {
                return null;
            }
            int home = rs.getInt("home_event_score");
            int away = rs.getInt("away_event_score");
            if (home + away <= 0) {
                return null;
            }
            return scoreboardFromNumbers(home, away, "MATCH_EVENTS");
        }, matchId);
    }

    private PublicScoreboard evidenceScoreboard(Long matchId) {
        if (matchId == null) {
            return null;
        }
        List<String> summaries = jdbcTemplate.query(
                "SELECT summary FROM source_evidence WHERE match_id=? AND summary IS NOT NULL ORDER BY evidence_time DESC, id DESC LIMIT 8",
                (rs, rowNum) -> rs.getString("summary"),
                matchId
        );
        for (String summary : summaries) {
            Matcher matcher = SCORE_PATTERN.matcher(summary == null ? "" : summary);
            if (matcher.find()) {
                return scoreboardFromNumbers(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), "EVIDENCE_TEXT");
            }
        }
        return null;
    }

    private PublicScoreboard scoreboardFromNumbers(int homeScore, int awayScore, String source) {
        String winnerSide;
        String resultText;
        if (homeScore > awayScore) {
            winnerSide = "HOME";
            resultText = "主队胜";
        } else if (awayScore > homeScore) {
            winnerSide = "AWAY";
            resultText = "客队胜";
        } else {
            winnerSide = "DRAW";
            resultText = "平局";
        }
        return new PublicScoreboard(homeScore, awayScore, homeScore + " - " + awayScore, winnerSide, resultText, source);
    }

    private record MatchRow(
            Long id,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard
    ) {
    }

    private record MarketRow(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            java.math.BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            long selectionCount,
            String sourceRef
    ) {
    }
}
