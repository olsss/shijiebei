package com.worldcup.profile.service;

import com.worldcup.profile.api.dto.ProfileDtos.PlayerProfileDetail;
import com.worldcup.profile.api.dto.ProfileDtos.PlayerProfileSummary;
import com.worldcup.profile.api.dto.ProfileDtos.ProfileFactResponse;
import com.worldcup.profile.api.dto.ProfileDtos.TeamPlayerResponse;
import com.worldcup.profile.api.dto.ProfileDtos.TeamProfileDetail;
import com.worldcup.profile.api.dto.ProfileDtos.TeamProfileSummary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProfileQueryService {
    private final JdbcTemplate jdbcTemplate;

    public ProfileQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<TeamProfileSummary> teams() {
        return jdbcTemplate.query(
                "SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags, t.attack_profile, t.defense_profile, t.public_sentiment, " +
                        "(SELECT COUNT(*) FROM players p WHERE p.team_id=t.id) AS player_count, " +
                        "(SELECT COUNT(*) FROM team_profile_facts f WHERE f.team_id=t.id) AS fact_count, " +
                        "(SELECT MAX(f.updated_at) FROM team_profile_facts f WHERE f.team_id=t.id) AS latest_profile_update " +
                        "FROM teams t ORDER BY t.display_name",
                teamMapper()
        );
    }

    @Transactional(readOnly = true)
    public TeamProfileDetail team(long teamId) {
        TeamProfileSummary summary = findTeam(teamId);
        return new TeamProfileDetail(
                summary,
                teamFacts(teamId),
                teamPlayers(teamId),
                evidenceCount(summary.teamKey()),
                conflictCount(summary.teamKey())
        );
    }

    @Transactional(readOnly = true)
    public List<TeamPlayerResponse> teamPlayers(long teamId) {
        return jdbcTemplate.query(
                "SELECT id, player_key, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status FROM players WHERE team_id=? ORDER BY shirt_number, display_name",
                (rs, rowNum) -> new TeamPlayerResponse(
                        rs.getLong("id"),
                        rs.getString("player_key"),
                        rs.getString("display_name"),
                        rs.getObject("shirt_number") == null ? null : rs.getInt("shirt_number"),
                        rs.getString("position"),
                        rs.getString("status"),
                        rs.getString("injury_status"),
                        rs.getString("card_status"),
                        rs.getString("locker_room_status")
                ),
                teamId
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerProfileSummary> players() {
        return jdbcTemplate.query(
                "SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, p.display_name, p.shirt_number, p.position, p.status, p.injury_status, p.card_status, p.locker_room_status, " +
                        "(SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id) AS fact_count, " +
                        "(SELECT MAX(f.updated_at) FROM player_profile_facts f WHERE f.player_id=p.id) AS latest_profile_update " +
                        "FROM players p LEFT JOIN teams t ON t.id=p.team_id ORDER BY p.display_name",
                playerMapper()
        );
    }

    @Transactional(readOnly = true)
    public PlayerProfileDetail player(long playerId) {
        PlayerProfileSummary summary = findPlayer(playerId);
        return new PlayerProfileDetail(summary, playerFacts(playerId));
    }

    private TeamProfileSummary findTeam(long teamId) {
        return jdbcTemplate.query(
                        "SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.style_tags, t.attack_profile, t.defense_profile, t.public_sentiment, " +
                                "(SELECT COUNT(*) FROM players p WHERE p.team_id=t.id) AS player_count, " +
                                "(SELECT COUNT(*) FROM team_profile_facts f WHERE f.team_id=t.id) AS fact_count, " +
                                "(SELECT MAX(f.updated_at) FROM team_profile_facts f WHERE f.team_id=t.id) AS latest_profile_update " +
                                "FROM teams t WHERE t.id=?",
                        teamMapper(),
                        teamId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "球队不存在"));
    }

    private PlayerProfileSummary findPlayer(long playerId) {
        return jdbcTemplate.query(
                        "SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, p.display_name, p.shirt_number, p.position, p.status, p.injury_status, p.card_status, p.locker_room_status, " +
                                "(SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id) AS fact_count, " +
                                "(SELECT MAX(f.updated_at) FROM player_profile_facts f WHERE f.player_id=p.id) AS latest_profile_update " +
                                "FROM players p LEFT JOIN teams t ON t.id=p.team_id WHERE p.id=?",
                        playerMapper(),
                        playerId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "球员不存在"));
    }

    private List<ProfileFactResponse> teamFacts(long teamId) {
        return jdbcTemplate.query(
                factSelect("team_profile_facts", "team_id") + " ORDER BY captured_at DESC, id DESC",
                factMapper(),
                teamId
        );
    }

    private List<ProfileFactResponse> playerFacts(long playerId) {
        return jdbcTemplate.query(
                factSelect("player_profile_facts", "player_id") + " ORDER BY captured_at DESC, id DESC",
                factMapper(),
                playerId
        );
    }

    private String factSelect(String table, String idColumn) {
        return "SELECT id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, source_name, source_url, source_ref, captured_at, approved_by FROM " +
                table + " WHERE " + idColumn + "=?";
    }

    private RowMapper<TeamProfileSummary> teamMapper() {
        return (rs, rowNum) -> new TeamProfileSummary(
                rs.getLong("id"),
                rs.getString("team_key"),
                rs.getString("display_name"),
                rs.getString("fifa_code"),
                rs.getString("country_region"),
                rs.getString("style_tags"),
                rs.getString("attack_profile"),
                rs.getString("defense_profile"),
                rs.getString("public_sentiment"),
                rs.getLong("player_count"),
                rs.getLong("fact_count"),
                rs.getTimestamp("latest_profile_update") == null ? null : rs.getTimestamp("latest_profile_update").toLocalDateTime()
        );
    }

    private RowMapper<PlayerProfileSummary> playerMapper() {
        return (rs, rowNum) -> new PlayerProfileSummary(
                rs.getLong("id"),
                rs.getString("player_key"),
                rs.getObject("team_id") == null ? null : rs.getLong("team_id"),
                rs.getString("team_name"),
                rs.getString("display_name"),
                rs.getObject("shirt_number") == null ? null : rs.getInt("shirt_number"),
                rs.getString("position"),
                rs.getString("status"),
                rs.getString("injury_status"),
                rs.getString("card_status"),
                rs.getString("locker_room_status"),
                rs.getLong("fact_count"),
                rs.getTimestamp("latest_profile_update") == null ? null : rs.getTimestamp("latest_profile_update").toLocalDateTime()
        );
    }

    private RowMapper<ProfileFactResponse> factMapper() {
        return (rs, rowNum) -> new ProfileFactResponse(
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
                rs.getTimestamp("captured_at") == null ? null : rs.getTimestamp("captured_at").toLocalDateTime(),
                rs.getString("approved_by")
        );
    }

    private long evidenceCount(String entityKey) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_evidence WHERE source_ref=?", Long.class, entityKey);
        return value == null ? 0 : value;
    }

    private long conflictCount(String entityKey) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM data_conflicts WHERE entity_key=?", Long.class, entityKey);
        return value == null ? 0 : value;
    }
}
