package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicProfileFact;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamExternalFactor;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamMatchHistory;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamPlayer;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamScoringPattern;
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
public class PublicProfilesService {
    private static final String APPROVED_FACT_CONDITION = "approved_by IS NOT NULL AND approved_by <> ''";

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicProfilesService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicTeamProfileSummary> teams() {
        return jdbcTemplate.query(teamSummarySelect() + " ORDER BY t.display_name",
                (rs, rowNum) -> teamSummary(rs).summary());
    }

    @Transactional(readOnly = true)
    public PublicTeamProfileDetail team(long teamId) {
        TeamSummaryRow row = findTeam(teamId);
        return new PublicTeamProfileDetail(
                row.summary(),
                teamFacts(teamId),
                teamPlayers(teamId),
                teamLineups(teamId),
                teamScoringPatterns(teamId),
                teamExternalFactors(teamId),
                teamMatchHistory(teamId),
                evidenceCount(row.rawTeamKey(), teamId),
                conflictCount(row.rawTeamKey())
        );
    }

    @Transactional(readOnly = true)
    public List<PublicTeamPlayer> teamPlayers(long teamId) {
        return jdbcTemplate.query("""
                SELECT id, player_key, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status
                FROM players
                WHERE team_id=?
                ORDER BY shirt_number, display_name
                """, (rs, rowNum) -> new PublicTeamPlayer(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("player_key")),
                mapper.sanitizeText(rs.getString("display_name")),
                nullableInt(rs, "shirt_number"),
                mapper.sanitizeText(rs.getString("position")),
                mapper.sanitizeToken(rs.getString("status")),
                mapper.sanitizeText(rs.getString("injury_status")),
                mapper.sanitizeText(rs.getString("card_status")),
                mapper.sanitizeText(rs.getString("locker_room_status"))
        ), teamId);
    }

    @Transactional(readOnly = true)
    public List<PublicPlayerProfileSummary> players() {
        return jdbcTemplate.query("""
                SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, p.display_name, p.shirt_number,
                       p.position, p.status, p.injury_status, p.card_status, p.locker_room_status,
                       (SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                       (SELECT MAX(f.updated_at) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS latest_profile_update
                FROM players p
                LEFT JOIN teams t ON t.id=p.team_id
                ORDER BY p.display_name
                """, (rs, rowNum) -> playerSummary(rs));
    }

    @Transactional(readOnly = true)
    public PublicPlayerProfileDetail player(long playerId) {
        PublicPlayerProfileSummary summary = findPlayer(playerId);
        return new PublicPlayerProfileDetail(summary, playerFacts(playerId));
    }

    private TeamSummaryRow findTeam(long teamId) {
        return jdbcTemplate.query(teamSummarySelect() + " WHERE t.id=?", (rs, rowNum) -> teamSummary(rs), teamId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found"));
    }

    private PublicPlayerProfileSummary findPlayer(long playerId) {
        return jdbcTemplate.query("""
                        SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, p.display_name, p.shirt_number,
                               p.position, p.status, p.injury_status, p.card_status, p.locker_room_status,
                               (SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                               (SELECT MAX(f.updated_at) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS latest_profile_update
                        FROM players p
                        LEFT JOIN teams t ON t.id=p.team_id
                        WHERE p.id=?
                        """,
                        (rs, rowNum) -> playerSummary(rs),
                        playerId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "player not found"));
    }

    private List<PublicProfileFact> teamFacts(long teamId) {
        return jdbcTemplate.query(factSelect("team_profile_facts", "team_id") + " ORDER BY captured_at DESC, id DESC",
                (rs, rowNum) -> fact(rs), teamId);
    }

    private List<PublicProfileFact> playerFacts(long playerId) {
        return jdbcTemplate.query(factSelect("player_profile_facts", "player_id") + " ORDER BY captured_at DESC, id DESC",
                (rs, rowNum) -> fact(rs), playerId);
    }

    private List<PublicTeamLineup> teamLineups(long teamId) {
        return jdbcTemplate.query("""
                SELECT l.match_id, m.match_name, m.matchday, l.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name,
                       l.role, l.position, l.is_starter
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                LEFT JOIN players p ON p.id=l.player_id
                WHERE l.team_id=?
                ORDER BY m.matchday DESC, l.is_starter DESC, p.shirt_number, p.display_name
                """, (rs, rowNum) -> new PublicTeamLineup(
                rs.getLong("match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name")),
                mapper.sanitizeToken(rs.getString("role")),
                mapper.sanitizeText(rs.getString("position")),
                rs.getBoolean("is_starter")
        ), teamId);
    }

    private List<PublicTeamScoringPattern> teamScoringPatterns(long teamId) {
        return jdbcTemplate.query("""
                SELECT s.match_id, m.match_name, m.matchday, s.goals_for, s.goals_against, s.first_goal_minute, s.scoring_minutes
                FROM match_team_stats s
                JOIN matches m ON m.id=s.match_id
                WHERE s.team_id=?
                ORDER BY m.matchday DESC, s.id DESC
                """, (rs, rowNum) -> new PublicTeamScoringPattern(
                rs.getLong("match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                nullableInt(rs, "goals_for"),
                nullableInt(rs, "goals_against"),
                nullableInt(rs, "first_goal_minute"),
                mapper.sanitizeText(rs.getString("scoring_minutes"))
        ), teamId);
    }

    private List<PublicTeamExternalFactor> teamExternalFactors(long teamId) {
        return jdbcTemplate.query("""
                SELECT id AS match_id, match_name, matchday, external_factors
                FROM matches
                WHERE (home_team_id=? OR away_team_id=?) AND external_factors IS NOT NULL AND external_factors <> ''
                ORDER BY matchday DESC, id DESC
                """, (rs, rowNum) -> new PublicTeamExternalFactor(
                rs.getLong("match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("external_factors"))
        ), teamId, teamId);
    }

    private List<PublicTeamMatchHistory> teamMatchHistory(long teamId) {
        return jdbcTemplate.query("""
                SELECT m.id AS match_id, m.match_name, m.matchday, m.competition, m.stage, m.venue, m.result_status,
                       s.goals_for, s.goals_against, s.scoring_minutes
                FROM matches m
                LEFT JOIN match_team_stats s ON s.match_id=m.id AND s.team_id=?
                WHERE m.home_team_id=? OR m.away_team_id=?
                ORDER BY m.matchday DESC, m.id DESC
                """, (rs, rowNum) -> new PublicTeamMatchHistory(
                rs.getLong("match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("competition")),
                mapper.sanitizeText(rs.getString("stage")),
                mapper.sanitizeText(rs.getString("venue")),
                mapper.sanitizeToken(rs.getString("result_status")),
                nullableInt(rs, "goals_for"),
                nullableInt(rs, "goals_against"),
                mapper.sanitizeText(rs.getString("scoring_minutes"))
        ), teamId, teamId, teamId);
    }

    private String teamSummarySelect() {
        return """
                SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags, t.attack_profile,
                       t.defense_profile, t.public_sentiment,
                       (SELECT COUNT(*) FROM players p WHERE p.team_id=t.id) AS player_count,
                       (SELECT COUNT(*) FROM team_profile_facts f WHERE f.team_id=t.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                       (SELECT MAX(f.updated_at) FROM team_profile_facts f WHERE f.team_id=t.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS latest_profile_update
                FROM teams t
                """;
    }

    private String factSelect(String table, String idColumn) {
        return "SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, "
                + "source_name, source_url, source_ref, captured_at FROM " + table + " WHERE " + idColumn + "=? AND " + APPROVED_FACT_CONDITION;
    }

    private TeamSummaryRow teamSummary(ResultSet rs) throws SQLException {
        String rawTeamKey = rs.getString("team_key");
        PublicTeamProfileSummary summary = new PublicTeamProfileSummary(
                rs.getLong("id"),
                mapper.sanitizeText(rawTeamKey),
                mapper.sanitizeText(rs.getString("display_name")),
                mapper.sanitizeText(rs.getString("fifa_code")),
                mapper.sanitizeText(rs.getString("country_region")),
                mapper.sanitizeText(rs.getString("style_tags")),
                mapper.sanitizeText(rs.getString("attack_profile")),
                mapper.sanitizeText(rs.getString("defense_profile")),
                mapper.sanitizeText(rs.getString("public_sentiment")),
                rs.getLong("player_count"),
                rs.getLong("fact_count"),
                localDateTime(rs, "latest_profile_update")
        );
        return new TeamSummaryRow(summary, rawTeamKey);
    }

    private PublicPlayerProfileSummary playerSummary(ResultSet rs) throws SQLException {
        return new PublicPlayerProfileSummary(
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
                mapper.sanitizeText(rs.getString("locker_room_status")),
                rs.getLong("fact_count"),
                localDateTime(rs, "latest_profile_update")
        );
    }

    private PublicProfileFact fact(ResultSet rs) throws SQLException {
        return new PublicProfileFact(
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

    private long evidenceCount(String entityKey, long teamId) {
        Long sourceEvidence = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_evidence WHERE source_ref=?", Long.class, entityKey);
        Long profileFacts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM team_profile_facts WHERE team_id=? AND source_name IS NOT NULL AND approved_by IS NOT NULL AND approved_by <> ''", Long.class, teamId);
        return (sourceEvidence == null ? 0 : sourceEvidence) + (profileFacts == null ? 0 : profileFacts);
    }

    private long conflictCount(String entityKey) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM data_conflicts WHERE entity_key=?", Long.class, entityKey);
        return value == null ? 0 : value;
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

    private record TeamSummaryRow(PublicTeamProfileSummary summary, String rawTeamKey) {
    }
}
