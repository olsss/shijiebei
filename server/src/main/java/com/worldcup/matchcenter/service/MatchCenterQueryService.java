package com.worldcup.matchcenter.service;

import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchConflictResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchDetailResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchEventResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchEvidenceResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchLineupResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchPlayerStatsResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchSummaryResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchTeamStatsResponse;
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
import java.util.List;

@Service
public class MatchCenterQueryService {
    private final JdbcTemplate jdbcTemplate;

    public MatchCenterQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> matches() {
        return jdbcTemplate.query(summarySelect() + " ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC", summaryMapper());
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse match(long matchId) {
        MatchSummaryResponse summary = findSummary(matchId);
        return new MatchDetailResponse(
                summary,
                externalFactors(matchId),
                lineups(matchId),
                events(matchId),
                teamStats(matchId),
                playerStats(matchId),
                evidence(matchId),
                conflicts(matchId)
        );
    }

    @Transactional(readOnly = true)
    public List<MatchLineupResponse> lineups(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query(
                "SELECT l.id, l.match_id, l.team_id, COALESCE(t.display_name, '未知球队') AS team_name, " +
                        "l.player_id, COALESCE(p.display_name, '未知球员') AS player_name, l.role, l.position, l.is_starter " +
                        "FROM match_lineups l JOIN matches m ON m.id=l.match_id " +
                        "LEFT JOIN teams t ON t.id=l.team_id LEFT JOIN players p ON p.id=l.player_id " +
                        "WHERE l.match_id=? " +
                        "ORDER BY CASE WHEN l.team_id=m.home_team_id THEN 0 ELSE 1 END, l.is_starter DESC, p.shirt_number, p.display_name, l.id",
                lineupMapper(),
                matchId
        );
    }

    @Transactional(readOnly = true)
    public List<MatchEventResponse> events(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query(
                "SELECT e.id, e.match_id, e.event_minute, e.event_type, e.team_id, COALESCE(t.display_name, '未知球队') AS team_name, " +
                        "e.player_id, COALESCE(p.display_name, '未知球员') AS player_name, e.payload " +
                        "FROM match_events e LEFT JOIN teams t ON t.id=e.team_id LEFT JOIN players p ON p.id=e.player_id " +
                        "WHERE e.match_id=? ORDER BY CASE WHEN e.event_minute IS NULL THEN 9999 ELSE e.event_minute END, e.id",
                eventMapper(),
                matchId
        );
    }

    @Transactional(readOnly = true)
    public List<MatchTeamStatsResponse> teamStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query(
                "SELECT s.id, s.match_id, s.team_id, COALESCE(t.display_name, '未知球队') AS team_name, s.stats_type, " +
                        "s.goals_for, s.goals_against, s.first_goal_minute, s.scoring_minutes, s.payload " +
                        "FROM match_team_stats s JOIN matches m ON m.id=s.match_id LEFT JOIN teams t ON t.id=s.team_id " +
                        "WHERE s.match_id=? ORDER BY CASE WHEN s.team_id=m.home_team_id THEN 0 ELSE 1 END, s.id",
                teamStatsMapper(),
                matchId
        );
    }

    @Transactional(readOnly = true)
    public List<MatchPlayerStatsResponse> playerStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query(
                "SELECT s.id, s.match_id, s.player_id, COALESCE(p.display_name, '未知球员') AS player_name, p.team_id, " +
                        "COALESCE(t.display_name, '未知球队') AS team_name, s.minutes_played, s.goals, s.assists, s.yellow_cards, s.red_cards, s.payload " +
                        "FROM match_player_stats s JOIN matches m ON m.id=s.match_id LEFT JOIN players p ON p.id=s.player_id LEFT JOIN teams t ON t.id=p.team_id " +
                        "WHERE s.match_id=? ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, s.id",
                playerStatsMapper(),
                matchId
        );
    }

    private MatchSummaryResponse findSummary(long matchId) {
        return jdbcTemplate.query(summarySelect() + " WHERE m.id=?", summaryMapper(), matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "比赛不存在"));
    }

    private void assertMatchExists(long matchId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE id=?", Long.class, matchId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "比赛不存在");
        }
    }

    private String externalFactors(long matchId) {
        return jdbcTemplate.queryForObject("SELECT external_factors FROM matches WHERE id=?", String.class, matchId);
    }

    private List<MatchEvidenceResponse> evidence(long matchId) {
        return jdbcTemplate.query(
                "SELECT id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score " +
                        "FROM source_evidence WHERE match_id=? ORDER BY evidence_time DESC, id DESC",
                evidenceMapper(),
                matchId
        );
    }

    private List<MatchConflictResponse> conflicts(long matchId) {
        return jdbcTemplate.query(
                "SELECT id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload " +
                        "FROM data_conflicts WHERE match_id=? ORDER BY id DESC",
                conflictMapper(),
                matchId
        );
    }

    private String summarySelect() {
        return "SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue, m.kickoff_time, " +
                "m.status, m.result_status, m.home_team_id, COALESCE(ht.display_name, '未知主队') AS home_team_name, " +
                "m.away_team_id, COALESCE(at.display_name, '未知客队') AS away_team_name, " +
                "(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id) AS event_count, " +
                "(SELECT COUNT(*) FROM match_lineups l WHERE l.match_id=m.id) AS lineup_count, " +
                "(SELECT COUNT(*) FROM source_evidence ev WHERE ev.match_id=m.id) AS evidence_count, " +
                "(SELECT COUNT(*) FROM data_conflicts c WHERE c.match_id=m.id) AS conflict_count " +
                "FROM matches m LEFT JOIN teams ht ON ht.id=m.home_team_id LEFT JOIN teams at ON at.id=m.away_team_id";
    }

    private RowMapper<MatchSummaryResponse> summaryMapper() {
        return (rs, rowNum) -> new MatchSummaryResponse(
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
                rs.getString("away_team_name"),
                rs.getLong("event_count"),
                rs.getLong("lineup_count"),
                rs.getLong("evidence_count"),
                rs.getLong("conflict_count")
        );
    }

    private RowMapper<MatchLineupResponse> lineupMapper() {
        return (rs, rowNum) -> new MatchLineupResponse(
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

    private RowMapper<MatchEventResponse> eventMapper() {
        return (rs, rowNum) -> new MatchEventResponse(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableInt(rs, "event_minute"),
                rs.getString("event_type"),
                nullableLong(rs, "team_id"),
                rs.getString("team_name"),
                nullableLong(rs, "player_id"),
                rs.getString("player_name"),
                rs.getString("payload")
        );
    }

    private RowMapper<MatchTeamStatsResponse> teamStatsMapper() {
        return (rs, rowNum) -> new MatchTeamStatsResponse(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                rs.getString("team_name"),
                rs.getString("stats_type"),
                nullableInt(rs, "goals_for"),
                nullableInt(rs, "goals_against"),
                nullableInt(rs, "first_goal_minute"),
                rs.getString("scoring_minutes"),
                rs.getString("payload")
        );
    }

    private RowMapper<MatchPlayerStatsResponse> playerStatsMapper() {
        return (rs, rowNum) -> new MatchPlayerStatsResponse(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "player_id"),
                rs.getString("player_name"),
                nullableLong(rs, "team_id"),
                rs.getString("team_name"),
                nullableInt(rs, "minutes_played"),
                nullableInt(rs, "goals"),
                nullableInt(rs, "assists"),
                nullableInt(rs, "yellow_cards"),
                nullableInt(rs, "red_cards"),
                rs.getString("payload")
        );
    }

    private RowMapper<MatchEvidenceResponse> evidenceMapper() {
        return (rs, rowNum) -> new MatchEvidenceResponse(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getString("source_name"),
                rs.getString("source_ref"),
                rs.getString("source_url"),
                localDateTime(rs, "evidence_time"),
                rs.getString("summary"),
                rs.getBigDecimal("reliability_score")
        );
    }

    private RowMapper<MatchConflictResponse> conflictMapper() {
        return (rs, rowNum) -> new MatchConflictResponse(
                rs.getLong("id"),
                rs.getString("conflict_type"),
                rs.getString("entity_key"),
                rs.getString("field_name"),
                rs.getString("current_value"),
                rs.getString("incoming_value"),
                rs.getString("resolution_status"),
                rs.getString("raw_payload")
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
