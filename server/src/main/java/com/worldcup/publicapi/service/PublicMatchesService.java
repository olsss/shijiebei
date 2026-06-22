package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchConflict;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvent;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvidence;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchPlayerStats;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchTeamStats;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicMatchesService {
    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicMatchesService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicMatchSummary> matches() {
        return jdbcTemplate.query(summarySelect() + " ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC",
                (rs, rowNum) -> summary(rs));
    }

    @Transactional(readOnly = true)
    public PublicMatchDetail match(long matchId) {
        PublicMatchSummary summary = findSummary(matchId);
        return new PublicMatchDetail(
                summary,
                mapper.sanitizeText(externalFactors(matchId)),
                lineups(matchId),
                events(matchId),
                teamStats(matchId),
                playerStats(matchId),
                evidence(matchId),
                conflicts(matchId)
        );
    }

    @Transactional(readOnly = true)
    public List<PublicMatchLineup> lineups(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT l.id, l.match_id, l.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name,
                       l.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name, l.role, l.position, l.is_starter
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                LEFT JOIN teams t ON t.id=l.team_id
                LEFT JOIN players p ON p.id=l.player_id
                WHERE l.match_id=?
                ORDER BY CASE WHEN l.team_id=m.home_team_id THEN 0 ELSE 1 END, l.is_starter DESC, p.shirt_number, p.display_name, l.id
                """, (rs, rowNum) -> new PublicMatchLineup(
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

    @Transactional(readOnly = true)
    public List<PublicMatchEvent> events(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT e.id, e.match_id, e.event_minute, e.event_type, e.team_id,
                       COALESCE(t.display_name, 'Unknown team') AS team_name,
                       e.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name
                FROM match_events e
                LEFT JOIN teams t ON t.id=e.team_id
                LEFT JOIN players p ON p.id=e.player_id
                WHERE e.match_id=?
                ORDER BY CASE WHEN e.event_minute IS NULL THEN 9999 ELSE e.event_minute END, e.id
                """, (rs, rowNum) -> new PublicMatchEvent(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableInt(rs, "event_minute"),
                mapper.sanitizeToken(rs.getString("event_type")),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name"))
        ), matchId);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchTeamStats> teamStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT s.id, s.match_id, s.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name, s.stats_type,
                       s.goals_for, s.goals_against, s.first_goal_minute, s.scoring_minutes
                FROM match_team_stats s
                JOIN matches m ON m.id=s.match_id
                LEFT JOIN teams t ON t.id=s.team_id
                WHERE s.match_id=?
                ORDER BY CASE WHEN s.team_id=m.home_team_id THEN 0 ELSE 1 END, s.id
                """, (rs, rowNum) -> new PublicMatchTeamStats(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                mapper.sanitizeToken(rs.getString("stats_type")),
                nullableInt(rs, "goals_for"),
                nullableInt(rs, "goals_against"),
                nullableInt(rs, "first_goal_minute"),
                mapper.sanitizeText(rs.getString("scoring_minutes"))
        ), matchId);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchPlayerStats> playerStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT s.id, s.match_id, s.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name, p.team_id,
                       COALESCE(t.display_name, 'Unknown team') AS team_name,
                       s.minutes_played, s.goals, s.assists, s.yellow_cards, s.red_cards
                FROM match_player_stats s
                JOIN matches m ON m.id=s.match_id
                LEFT JOIN players p ON p.id=s.player_id
                LEFT JOIN teams t ON t.id=p.team_id
                WHERE s.match_id=?
                ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, s.id
                """, (rs, rowNum) -> new PublicMatchPlayerStats(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name")),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableInt(rs, "minutes_played"),
                nullableInt(rs, "goals"),
                nullableInt(rs, "assists"),
                nullableInt(rs, "yellow_cards"),
                nullableInt(rs, "red_cards")
        ), matchId);
    }

    private PublicMatchSummary findSummary(long matchId) {
        return jdbcTemplate.query(summarySelect() + " WHERE m.id=?", (rs, rowNum) -> summary(rs), matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private void assertMatchExists(long matchId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE id=?", Long.class, matchId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found");
        }
    }

    private String externalFactors(long matchId) {
        return jdbcTemplate.queryForObject("SELECT external_factors FROM matches WHERE id=?", String.class, matchId);
    }

    private List<PublicMatchEvidence> evidence(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score
                FROM source_evidence
                WHERE match_id=?
                ORDER BY evidence_time DESC, id DESC
                """, (rs, rowNum) -> new PublicMatchEvidence(
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

    private List<PublicMatchConflict> conflicts(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, conflict_type, entity_key, field_name, resolution_status
                FROM data_conflicts
                WHERE match_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new PublicMatchConflict(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("conflict_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeToken(rs.getString("field_name")),
                mapper.sanitizeToken(rs.getString("resolution_status"))
        ), matchId);
    }

    private String summarySelect() {
        return """
                SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue, m.kickoff_time,
                       m.status, m.result_status, m.home_team_id, COALESCE(ht.display_name, 'Unknown home team') AS home_team_name,
                       m.away_team_id, COALESCE(at.display_name, 'Unknown away team') AS away_team_name,
                       (SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id) AS event_count,
                       (SELECT COUNT(*) FROM match_lineups l WHERE l.match_id=m.id) AS lineup_count,
                       (SELECT COUNT(*) FROM source_evidence ev WHERE ev.match_id=m.id) AS evidence_count,
                       (SELECT COUNT(*) FROM data_conflicts c WHERE c.match_id=m.id) AS conflict_count
                FROM matches m
                LEFT JOIN teams ht ON ht.id=m.home_team_id
                LEFT JOIN teams at ON at.id=m.away_team_id
                """;
    }

    private PublicMatchSummary summary(ResultSet rs) throws SQLException {
        return new PublicMatchSummary(
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
                rs.getLong("event_count"),
                rs.getLong("lineup_count"),
                rs.getLong("evidence_count"),
                rs.getLong("conflict_count")
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
}
