package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerMetricSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicProfileFact;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicProfileReadiness;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamExternalFactor;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamMatchHistory;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamMetricSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamPlayer;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamScoringPattern;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamVisual;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicProfilesService {
    private static final String APPROVED_FACT_CONDITION = "approved_by IS NOT NULL AND approved_by <> ''";
    private static final BigDecimal DERIVED_FACT_SCORE = BigDecimal.valueOf(7.0);

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
        List<PublicProfileFact> approvedFacts = teamFacts(teamId);
        List<PublicTeamPlayer> players = teamPlayers(teamId);
        List<PublicTeamLineup> lineups = teamLineups(teamId);
        List<PublicTeamScoringPattern> scoringPatterns = teamScoringPatterns(teamId);
        List<PublicTeamExternalFactor> externalFactors = teamExternalFactors(teamId);
        List<PublicTeamMatchHistory> matchHistory = teamMatchHistory(teamId);
        long evidenceCount = evidenceCount(row.rawTeamKey(), teamId);
        long conflictCount = conflictCount(row.rawTeamKey());
        PublicTeamMetricSummary latestMetric = latestTeamMetric(teamId);
        List<PublicProfileFact> facts = withDerivedTeamFacts(row.summary(), approvedFacts, players, lineups, scoringPatterns, externalFactors, matchHistory, latestMetric);
        return new PublicTeamProfileDetail(
                row.summary(),
                facts,
                players,
                lineups,
                scoringPatterns,
                externalFactors,
                matchHistory,
                evidenceCount,
                conflictCount,
                teamReadiness(row.summary(), facts, players, lineups, scoringPatterns, externalFactors, matchHistory, evidenceCount, conflictCount, latestMetric),
                latestMetric
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
                SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, t.fifa_code AS team_fifa_code,
                       t.country_iso2 AS team_country_iso2, t.flag_asset_key AS team_flag_asset_key, t.country_region AS team_country_region,
                       p.display_name, p.shirt_number,
                       p.position, p.status, p.injury_status, p.card_status, p.locker_room_status,
                       (SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                       (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id) AS metric_count,
                       (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id
                           AND (s.minutes_played IS NOT NULL OR s.goals IS NOT NULL OR s.assists IS NOT NULL
                                OR s.shots IS NOT NULL OR s.shots_on_target IS NOT NULL)) AS performance_metric_count,
                       (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id
                           AND (s.xg IS NOT NULL OR s.xa IS NOT NULL OR s.npxg IS NOT NULL
                                OR s.key_passes IS NOT NULL OR s.progressive_passes IS NOT NULL
                                OR s.training_load IS NOT NULL OR s.availability_score IS NOT NULL
                                OR s.expected_starting_probability IS NOT NULL)) AS advanced_metric_count,
                       (SELECT COUNT(DISTINCT l.match_id) FROM match_lineups l WHERE l.player_id=p.id) AS lineup_match_count,
                       (SELECT COUNT(*) FROM match_events e WHERE e.player_id=p.id) AS event_count,
                       (SELECT MAX(f.updated_at) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS latest_profile_update
                FROM players p
                LEFT JOIN teams t ON t.id=p.team_id
                ORDER BY p.display_name
                """, (rs, rowNum) -> playerSummary(rs));
    }

    @Transactional(readOnly = true)
    public PublicPlayerProfileDetail player(long playerId) {
        PublicPlayerProfileSummary summary = findPlayer(playerId);
        List<PublicProfileFact> approvedFacts = playerFacts(playerId);
        PublicPlayerMetricSummary latestMetric = latestPlayerMetric(playerId);
        PlayerLineupParticipation lineupParticipation = playerLineupParticipation(playerId);
        PlayerEventParticipation eventParticipation = playerEventParticipation(playerId);
        List<PublicProfileFact> facts = withDerivedPlayerFacts(summary, approvedFacts, latestMetric, lineupParticipation, eventParticipation);
        return new PublicPlayerProfileDetail(summary, facts, playerReadiness(summary, facts, latestMetric), latestMetric);
    }

    private TeamSummaryRow findTeam(long teamId) {
        return jdbcTemplate.query(teamSummarySelect() + " WHERE t.id=?", (rs, rowNum) -> teamSummary(rs), teamId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found"));
    }

    private PublicPlayerProfileSummary findPlayer(long playerId) {
        return jdbcTemplate.query("""
                        SELECT p.id, p.player_key, p.team_id, t.display_name AS team_name, t.fifa_code AS team_fifa_code,
                               t.country_iso2 AS team_country_iso2, t.flag_asset_key AS team_flag_asset_key, t.country_region AS team_country_region,
                               p.display_name, p.shirt_number,
                               p.position, p.status, p.injury_status, p.card_status, p.locker_room_status,
                               (SELECT COUNT(*) FROM player_profile_facts f WHERE f.player_id=p.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                               (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id) AS metric_count,
                               (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id
                                   AND (s.minutes_played IS NOT NULL OR s.goals IS NOT NULL OR s.assists IS NOT NULL
                                        OR s.shots IS NOT NULL OR s.shots_on_target IS NOT NULL)) AS performance_metric_count,
                               (SELECT COUNT(*) FROM player_metric_snapshots s WHERE s.player_id=p.id
                                   AND (s.xg IS NOT NULL OR s.xa IS NOT NULL OR s.npxg IS NOT NULL
                                        OR s.key_passes IS NOT NULL OR s.progressive_passes IS NOT NULL
                                        OR s.training_load IS NOT NULL OR s.availability_score IS NOT NULL
                                        OR s.expected_starting_probability IS NOT NULL)) AS advanced_metric_count,
                               (SELECT COUNT(DISTINCT l.match_id) FROM match_lineups l WHERE l.player_id=p.id) AS lineup_match_count,
                               (SELECT COUNT(*) FROM match_events e WHERE e.player_id=p.id) AS event_count,
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
                SELECT t.id, t.team_key, t.display_name, t.fifa_code, t.country_region, t.country_iso2, t.flag_asset_key, t.confederation, t.group_name, t.metadata_source_ref, t.style_tags, t.attack_profile,
                       t.defense_profile, t.public_sentiment,
                       (SELECT COUNT(*) FROM players p WHERE p.team_id=t.id) AS player_count,
                       (SELECT COUNT(DISTINCT l.match_id) FROM match_lineups l WHERE l.team_id=t.id) AS lineup_match_count,
                       (SELECT COUNT(*) FROM match_team_stats s WHERE s.team_id=t.id) AS scoring_count,
                       (SELECT COUNT(*) FROM matches m WHERE m.home_team_id=t.id OR m.away_team_id=t.id) AS match_count,
                       (SELECT COUNT(*) FROM matches m WHERE (m.home_team_id=t.id OR m.away_team_id=t.id) AND m.external_factors IS NOT NULL AND m.external_factors <> '') AS external_factor_count,
                       (SELECT COUNT(*) FROM team_metric_snapshots s WHERE s.team_id=t.id) AS metric_count,
                       (SELECT COUNT(*) FROM team_metric_snapshots s WHERE s.team_id=t.id
                           AND (s.shots IS NOT NULL OR s.shots_on_target IS NOT NULL OR s.possession_pct IS NOT NULL)) AS technical_metric_count,
                       (SELECT COUNT(*) FROM team_metric_snapshots s WHERE s.team_id=t.id
                           AND (s.xg IS NOT NULL OR s.xga IS NOT NULL OR s.npxg IS NOT NULL OR s.ppda IS NOT NULL OR s.xpts IS NOT NULL
                                OR s.progressive_passes IS NOT NULL OR s.set_piece_xg IS NOT NULL OR s.form_score IS NOT NULL)) AS advanced_metric_count,
                       (SELECT COUNT(*) FROM team_profile_facts f WHERE f.team_id=t.id AND f.approved_by IS NOT NULL AND f.approved_by <> '') AS fact_count,
                       (SELECT f.summary FROM team_profile_facts f
                        WHERE f.team_id=t.id AND f.fact_type='GROUP_STANDING_SNAPSHOT'
                          AND f.approved_by IS NOT NULL AND f.approved_by <> ''
                        ORDER BY f.captured_at DESC, f.id DESC LIMIT 1) AS group_standing_summary,
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
        String displayName = rs.getString("display_name");
        String fifaCode = rs.getString("fifa_code");
        String countryRegion = rs.getString("country_region");
        String countryIso2 = rs.getString("country_iso2");
        String flagAssetKey = rs.getString("flag_asset_key");
        String confederation = rs.getString("confederation");
        String metadataSourceRef = rs.getString("metadata_source_ref");
        String attackProfile = rs.getString("attack_profile");
        String defenseProfile = rs.getString("defense_profile");
        String publicSentiment = rs.getString("public_sentiment");
        String styleTags = readableStyleTags(rs.getString("style_tags"));
        String groupName = readableGroupName(rs.getString("group_name"), styleTags);
        long playerCount = rs.getLong("player_count");
        String groupStandingSummary = rs.getString("group_standing_summary");
        long factCount = rs.getLong("fact_count") + derivedTeamFactCount(
                fifaCode,
                countryRegion,
                countryIso2,
                flagAssetKey,
                confederation,
                groupName,
                metadataSourceRef,
                styleTags,
                attackProfile,
                defenseProfile,
                publicSentiment,
                playerCount,
                rs.getLong("lineup_match_count"),
                rs.getLong("scoring_count"),
                rs.getLong("match_count"),
                rs.getLong("external_factor_count"),
                rs.getLong("metric_count")
        );
        PublicTeamProfileSummary summary = new PublicTeamProfileSummary(
                rs.getLong("id"),
                mapper.sanitizeText(rawTeamKey),
                mapper.sanitizeText(displayName),
                mapper.sanitizeText(fifaCode),
                mapper.sanitizeText(countryRegion),
                mapper.sanitizeToken(countryIso2),
                mapper.sanitizeText(flagAssetKey),
                mapper.sanitizeToken(confederation),
                mapper.sanitizeText(groupName),
                mapper.sanitizeText(metadataSourceRef),
                mapper.sanitizeText(styleTags),
                mapper.sanitizeText(attackProfile),
                mapper.sanitizeText(defenseProfile),
                mapper.sanitizeText(publicSentiment),
                playerCount,
                factCount,
                rs.getLong("technical_metric_count"),
                rs.getLong("advanced_metric_count"),
                parseGroupStandingRank(groupStandingSummary),
                parseGroupStandingPoints(groupStandingSummary),
                parseGroupStandingRecord(groupStandingSummary),
                parseGroupGoalDifference(groupStandingSummary),
                mapper.sanitizeText(groupStandingSummary),
                localDateTime(rs, "latest_profile_update")
        );
        return new TeamSummaryRow(summary, rawTeamKey);
    }

    private Integer parseGroupStandingRank(String summary) {
        return parseFirstInteger(summary, "第(\\d+)/(?:\\d+)名");
    }

    private Integer parseGroupStandingPoints(String summary) {
        return parseFirstInteger(summary, "积分(\\d+)");
    }

    private Integer parseGroupGoalDifference(String summary) {
        return parseFirstInteger(summary, "净胜球([+-]?\\d+)");
    }

    private String parseGroupStandingRecord(String summary) {
        if (!hasText(summary)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+场\\d+胜\\d+平\\d+负)").matcher(summary);
        return matcher.find() ? mapper.sanitizeText(matcher.group(1)) : null;
    }

    private Integer parseFirstInteger(String summary, String pattern) {
        if (!hasText(summary)) {
            return null;
        }
        Matcher matcher = Pattern.compile(pattern).matcher(summary);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String readableStyleTags(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String value = rawValue.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            String content = value.substring(1, value.length() - 1).trim();
            if (content.isBlank()) {
                return null;
            }
            String[] tokens = content.split(",");
            ArrayList<String> parts = new ArrayList<>();
            for (String token : tokens) {
                String cleaned = token.trim()
                        .replaceAll("^['\\\"]+", "")
                        .replaceAll("['\\\"]+$", "")
                        .replace("\\\"", "\"")
                        .trim();
                if (!cleaned.isBlank()) {
                    parts.add(cleaned);
                }
            }
            return parts.isEmpty() ? null : String.join(" · ", parts);
        }
        return value;
    }

    private String readableGroupName(String groupName, String styleTags) {
        if (hasText(groupName)) {
            return groupName.trim();
        }
        if (!hasText(styleTags)) {
            return null;
        }
        String[] parts = styleTags.split("[·,，、/|\\s]+");
        for (String part : parts) {
            String candidate = part.trim();
            if (candidate.matches("(?i)^[A-Z]组$") || candidate.matches("(?i)^Group[A-Z]$")) {
                return candidate;
            }
        }
        return null;
    }

    private PublicPlayerProfileSummary playerSummary(ResultSet rs) throws SQLException {
        Long teamId = nullableLong(rs, "team_id");
        String teamName = rs.getString("team_name");
        String teamFifaCode = rs.getString("team_fifa_code");
        String teamCountryIso2 = rs.getString("team_country_iso2");
        String teamFlagAssetKey = rs.getString("team_flag_asset_key");
        String teamCountryRegion = rs.getString("team_country_region");
        Integer shirtNumber = nullableInt(rs, "shirt_number");
        String position = rs.getString("position");
        String status = rs.getString("status");
        String injuryStatus = rs.getString("injury_status");
        String cardStatus = rs.getString("card_status");
        String lockerRoomStatus = rs.getString("locker_room_status");
        long factCount = rs.getLong("fact_count") + derivedPlayerFactCount(
                teamId,
                teamName,
                teamFifaCode,
                teamCountryIso2,
                teamFlagAssetKey,
                teamCountryRegion,
                shirtNumber,
                position,
                status,
                injuryStatus,
                cardStatus,
                lockerRoomStatus,
                rs.getLong("lineup_match_count"),
                rs.getLong("event_count"),
                rs.getLong("metric_count")
        );
        return new PublicPlayerProfileSummary(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("player_key")),
                teamId,
                mapper.sanitizeText(teamName),
                teamVisual(
                        teamId,
                        teamName,
                        teamFifaCode,
                        teamCountryIso2,
                        teamFlagAssetKey,
                        teamCountryRegion
                ),
                mapper.sanitizeText(rs.getString("display_name")),
                shirtNumber,
                mapper.sanitizeText(position),
                mapper.sanitizeToken(status),
                mapper.sanitizeText(injuryStatus),
                mapper.sanitizeText(cardStatus),
                mapper.sanitizeText(lockerRoomStatus),
                factCount,
                rs.getLong("performance_metric_count"),
                rs.getLong("advanced_metric_count"),
                localDateTime(rs, "latest_profile_update")
        );
    }

    private PublicTeamVisual teamVisual(Long teamId, String teamName, String fifaCode, String countryIso2, String flagAssetKey, String countryRegion) {
        return new PublicTeamVisual(
                teamId,
                mapper.sanitizeText(teamName),
                mapper.sanitizeToken(fifaCode),
                mapper.sanitizeToken(countryIso2),
                mapper.sanitizeText(flagAssetKey),
                mapper.sanitizeText(countryRegion)
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

    private PublicTeamMetricSummary latestTeamMetric(long teamId) {
        return jdbcTemplate.query("""
                SELECT xg, xga, npxg, ppda, xpts, shots, shots_on_target, possession_pct, progressive_passes,
                       set_piece_xg, form_score, source_name, source_ref, captured_at
                FROM team_metric_snapshots
                WHERE team_id=?
                ORDER BY captured_at DESC, id DESC
                LIMIT 1
                """, (rs, rowNum) -> new PublicTeamMetricSummary(
                rs.getBigDecimal("xg"),
                rs.getBigDecimal("xga"),
                rs.getBigDecimal("npxg"),
                rs.getBigDecimal("ppda"),
                rs.getBigDecimal("xpts"),
                nullableInt(rs, "shots"),
                nullableInt(rs, "shots_on_target"),
                rs.getBigDecimal("possession_pct"),
                nullableInt(rs, "progressive_passes"),
                rs.getBigDecimal("set_piece_xg"),
                rs.getBigDecimal("form_score"),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "captured_at")
        ), teamId).stream().findFirst().orElse(null);
    }

    private PublicPlayerMetricSummary latestPlayerMetric(long playerId) {
        return jdbcTemplate.query("""
                SELECT minutes_played, goals, assists, xg, xa, npxg, shots, shots_on_target, key_passes,
                       progressive_passes, training_load, availability_score, expected_starting_probability,
                       source_name, source_ref, captured_at
                FROM player_metric_snapshots
                WHERE player_id=?
                ORDER BY captured_at DESC, id DESC
                LIMIT 1
                """, (rs, rowNum) -> new PublicPlayerMetricSummary(
                nullableInt(rs, "minutes_played"),
                rs.getBigDecimal("goals"),
                rs.getBigDecimal("assists"),
                rs.getBigDecimal("xg"),
                rs.getBigDecimal("xa"),
                rs.getBigDecimal("npxg"),
                nullableInt(rs, "shots"),
                nullableInt(rs, "shots_on_target"),
                nullableInt(rs, "key_passes"),
                nullableInt(rs, "progressive_passes"),
                rs.getBigDecimal("training_load"),
                rs.getBigDecimal("availability_score"),
                rs.getBigDecimal("expected_starting_probability"),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "captured_at")
        ), playerId).stream().findFirst().orElse(null);
    }

    private PlayerLineupParticipation playerLineupParticipation(long playerId) {
        return jdbcTemplate.query("""
                SELECT l.match_id,
                       m.match_name,
                       m.matchday,
                       l.role,
                       l.position,
                       (SELECT COUNT(DISTINCT c.match_id) FROM match_lineups c WHERE c.player_id=?) AS match_count,
                       (SELECT COALESCE(SUM(CASE WHEN c.is_starter = TRUE THEN 1 ELSE 0 END), 0) FROM match_lineups c WHERE c.player_id=?) AS starter_count
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                WHERE l.player_id=?
                ORDER BY m.matchday DESC, l.id DESC
                LIMIT 1
                """, (rs, rowNum) -> new PlayerLineupParticipation(
                rs.getLong("match_count"),
                rs.getLong("starter_count"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeToken(rs.getString("role")),
                mapper.sanitizeText(rs.getString("position"))
        ), playerId, playerId, playerId).stream().findFirst().orElse(PlayerLineupParticipation.empty());
    }

    private PlayerEventParticipation playerEventParticipation(long playerId) {
        return jdbcTemplate.query("""
                SELECT e.event_type,
                       e.event_minute,
                       m.match_name,
                       m.matchday,
                       (SELECT COUNT(*) FROM match_events c WHERE c.player_id=?) AS event_count,
                       (SELECT COALESCE(SUM(CASE WHEN c.event_type LIKE '%GOAL%' OR c.event_type='PENALTY_SCORED' THEN 1 ELSE 0 END), 0) FROM match_events c WHERE c.player_id=?) AS goal_event_count,
                       (SELECT COALESCE(SUM(CASE WHEN c.event_type IN ('YELLOW_CARD','RED_CARD','VAR_RED_CARD_UPGRADE') THEN 1 ELSE 0 END), 0) FROM match_events c WHERE c.player_id=?) AS card_event_count
                FROM match_events e
                JOIN matches m ON m.id=e.match_id
                WHERE e.player_id=?
                ORDER BY m.matchday DESC, e.event_minute DESC, e.id DESC
                LIMIT 1
                """, (rs, rowNum) -> new PlayerEventParticipation(
                rs.getLong("event_count"),
                rs.getLong("goal_event_count"),
                rs.getLong("card_event_count"),
                mapper.sanitizeToken(rs.getString("event_type")),
                nullableInt(rs, "event_minute"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday")
        ), playerId, playerId, playerId, playerId).stream().findFirst().orElse(PlayerEventParticipation.empty());
    }

    private List<PublicProfileFact> withDerivedTeamFacts(
            PublicTeamProfileSummary team,
            List<PublicProfileFact> approvedFacts,
            List<PublicTeamPlayer> players,
            List<PublicTeamLineup> lineups,
            List<PublicTeamScoringPattern> scoringPatterns,
            List<PublicTeamExternalFactor> externalFactors,
            List<PublicTeamMatchHistory> matchHistory,
            PublicTeamMetricSummary latestMetric
    ) {
        ArrayList<PublicProfileFact> facts = new ArrayList<>(approvedFacts);
        long derivedId = -1001L;
        if (hasAnyText(team.fifaCode(), team.countryRegion(), team.countryIso2(), team.flagAssetKey(), team.confederation(), team.groupName(), team.metadataSourceRef())) {
            facts.add(derivedFact(derivedId--, "PROFILE", "国家队上下文", teamContextSummary(team), "teams"));
        }
        if (hasAnyText(team.styleTags(), team.attackProfile(), team.defenseProfile(), team.publicSentiment())) {
            facts.add(derivedFact(derivedId--, "STYLE", "基础画像字段", teamProfileSummary(team), "teams"));
        }
        if (!players.isEmpty()) {
            facts.add(derivedFact(derivedId--, "PROFILE", "名单覆盖", "已关联 " + players.size() + " 名球员，号码、位置和名单状态来自正式球员表。", "players"));
        }
        if (!lineups.isEmpty()) {
            facts.add(derivedFact(derivedId--, "TACTIC", "阵容记录", "已关联 " + distinctLineupMatchCount(lineups) + " 场比赛的阵容/首发记录。", "match_lineups"));
        }
        if (!scoringPatterns.isEmpty()) {
            facts.add(derivedFact(derivedId--, "FORM", "近期比分", teamScoringSummary(scoringPatterns), "match_team_stats"));
        }
        if (!matchHistory.isEmpty()) {
            facts.add(derivedFact(derivedId--, "HISTORY", "赛程赛果", "已关联 " + matchHistory.size() + " 场比赛，阶段、场地和赛果状态来自正式比赛表。", "matches"));
        }
        if (!externalFactors.isEmpty()) {
            facts.add(derivedFact(derivedId--, "OTHER", "外部因素字段", "已有 " + externalFactors.size() + " 场比赛的外部因素文本，字段来自正式比赛表。", "matches.external_factors"));
        }
        if (latestMetric != null) {
            facts.add(derivedFact(derivedId, "FORM", "技术指标字段", teamMetricSummary(latestMetric), "team_metric_snapshots"));
        }
        return facts;
    }

    private List<PublicProfileFact> withDerivedPlayerFacts(
            PublicPlayerProfileSummary player,
            List<PublicProfileFact> approvedFacts,
            PublicPlayerMetricSummary latestMetric,
            PlayerLineupParticipation lineupParticipation,
            PlayerEventParticipation eventParticipation
    ) {
        ArrayList<PublicProfileFact> facts = new ArrayList<>(approvedFacts);
        long derivedId = -2001L;
        if (player.teamId() != null || player.team() != null && hasAnyText(
                player.team().teamName(),
                player.team().fifaCode(),
                player.team().countryIso2(),
                player.team().flagUrl(),
                player.team().countryRegion()
        )) {
            facts.add(derivedFact(derivedId--, "PROFILE", "国家队归属", playerTeamSummary(player), "players.team_id"));
        }
        if (player.shirtNumber() != null || hasAnyText(player.position(), player.status())) {
            facts.add(derivedFact(derivedId--, "PROFILE", "名单身份", playerIdentitySummary(player), "players"));
        }
        if (lineupParticipation != null && lineupParticipation.matchCount() > 0) {
            facts.add(derivedFact(derivedId--, "FORM", "阵容参与", playerLineupSummary(lineupParticipation), "match_lineups"));
        }
        if (eventParticipation != null && eventParticipation.eventCount() > 0) {
            facts.add(derivedFact(derivedId--, "FORM", "比赛事件", playerEventSummary(eventParticipation), "match_events"));
        }
        if (hasAnyText(player.injuryStatus(), player.cardStatus(), player.lockerRoomStatus())) {
            facts.add(derivedFact(derivedId--, "INJURY", "可用性字段", playerAvailabilitySummary(player), "players.availability"));
        }
        if (latestMetric != null) {
            facts.add(derivedFact(derivedId, "FORM", "表现指标字段", playerMetricSummary(latestMetric), "player_metric_snapshots"));
        }
        return facts;
    }

    private long derivedTeamFactCount(
            String fifaCode,
            String countryRegion,
            String countryIso2,
            String flagAssetKey,
            String confederation,
            String groupName,
            String metadataSourceRef,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment,
            long playerCount,
            long lineupMatchCount,
            long scoringCount,
            long matchCount,
            long externalFactorCount,
            long metricCount
    ) {
        long count = 0;
        if (hasAnyText(fifaCode, countryRegion, countryIso2, flagAssetKey, confederation, groupName, metadataSourceRef)) {
            count++;
        }
        if (hasAnyText(styleTags, attackProfile, defenseProfile, publicSentiment)) {
            count++;
        }
        if (playerCount > 0) {
            count++;
        }
        if (lineupMatchCount > 0) {
            count++;
        }
        if (scoringCount > 0) {
            count++;
        }
        if (matchCount > 0) {
            count++;
        }
        if (externalFactorCount > 0) {
            count++;
        }
        if (metricCount > 0) {
            count++;
        }
        return count;
    }

    private long derivedPlayerFactCount(
            Long teamId,
            String teamName,
            String teamFifaCode,
            String teamCountryIso2,
            String teamFlagAssetKey,
            String teamCountryRegion,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus,
            long lineupMatchCount,
            long eventCount,
            long metricCount
    ) {
        long count = 0;
        if (teamId != null || hasAnyText(teamName, teamFifaCode, teamCountryIso2, teamFlagAssetKey, teamCountryRegion)) {
            count++;
        }
        if (shirtNumber != null || hasAnyText(position, status)) {
            count++;
        }
        if (hasAnyText(injuryStatus, cardStatus, lockerRoomStatus)) {
            count++;
        }
        if (lineupMatchCount > 0) {
            count++;
        }
        if (eventCount > 0) {
            count++;
        }
        if (metricCount > 0) {
            count++;
        }
        return count;
    }

    private PublicProfileFact derivedFact(long id, String factType, String title, String summary, String sourceRef) {
        return new PublicProfileFact(
                id,
                factType,
                null,
                mapper.sanitizeText(title),
                mapper.sanitizeText(summary),
                null,
                DERIVED_FACT_SCORE,
                DERIVED_FACT_SCORE,
                "正式库派生",
                null,
                sourceRef,
                null
        );
    }

    private String teamContextSummary(PublicTeamProfileSummary team) {
        ArrayList<String> parts = new ArrayList<>();
        addPart(parts, "FIFA", team.fifaCode());
        addPart(parts, "国家/地区", team.countryRegion());
        addPart(parts, "ISO", team.countryIso2());
        addPart(parts, "国旗", team.flagAssetKey());
        addPart(parts, "洲际", team.confederation());
        addPart(parts, "小组", team.groupName());
        return parts.isEmpty() ? "国家队基础字段来自正式球队表。" : String.join("，", parts) + "。";
    }

    private String teamProfileSummary(PublicTeamProfileSummary team) {
        ArrayList<String> parts = new ArrayList<>();
        if (hasText(team.styleTags())) {
            parts.add("风格标签");
        }
        if (hasText(team.attackProfile())) {
            parts.add("进攻画像");
        }
        if (hasText(team.defenseProfile())) {
            parts.add("防守画像");
        }
        if (hasText(team.publicSentiment())) {
            parts.add("公众舆情");
        }
        return parts.isEmpty() ? "球队画像字段来自正式球队表。" : String.join("、", parts) + "字段已入库。";
    }

    private String teamScoringSummary(List<PublicTeamScoringPattern> scoringPatterns) {
        PublicTeamScoringPattern latest = scoringPatterns.get(0);
        StringBuilder summary = new StringBuilder("已关联 ")
                .append(scoringPatterns.size())
                .append(" 场球队进失球记录");
        if (latest.goalsFor() != null && latest.goalsAgainst() != null) {
            summary.append("，最近记录 ")
                    .append(latest.goalsFor())
                    .append("-")
                    .append(latest.goalsAgainst());
        }
        if (hasText(latest.scoringMinutes())) {
            summary.append("，进球时间 ")
                    .append(latest.scoringMinutes());
        }
        summary.append("。");
        return summary.toString();
    }

    private String teamMetricSummary(PublicTeamMetricSummary metric) {
        ArrayList<String> parts = new ArrayList<>();
        if (metric.xg() != null) {
            parts.add("xG");
        }
        if (metric.xga() != null) {
            parts.add("xGA");
        }
        if (metric.ppda() != null) {
            parts.add("PPDA");
        }
        if (metric.shots() != null) {
            parts.add("射门");
        }
        if (metric.possessionPct() != null) {
            parts.add("控球");
        }
        if (metric.setPieceXg() != null) {
            parts.add("定位球 xG");
        }
        return parts.isEmpty()
                ? "最新指标快照已入库。"
                : teamMetricHasAdvanced(metric)
                        ? "最新高阶/基础指标包含 " + String.join("、", parts) + "。"
                        : "最新比赛级基础指标包含 " + String.join("、", parts) + "；xG、PPDA 等专业高阶指标仍待补。";
    }

    private String playerTeamSummary(PublicPlayerProfileSummary player) {
        PublicTeamVisual team = player.team();
        if (team == null) {
            return "球员已关联正式球队编号。";
        }
        ArrayList<String> parts = new ArrayList<>();
        addPart(parts, "球队", team.teamName());
        addPart(parts, "FIFA", team.fifaCode());
        addPart(parts, "ISO", team.countryIso2());
        addPart(parts, "国旗", team.flagUrl());
        addPart(parts, "地区", team.countryRegion());
        return parts.isEmpty() ? "球员已关联正式球队编号。" : String.join("，", parts) + "。";
    }

    private String playerIdentitySummary(PublicPlayerProfileSummary player) {
        ArrayList<String> parts = new ArrayList<>();
        if (player.shirtNumber() != null) {
            parts.add("球衣 " + player.shirtNumber() + " 号");
        }
        addPart(parts, "位置", player.position());
        addPart(parts, "名单状态", player.status());
        return parts.isEmpty() ? "球员名单身份来自正式球员表。" : String.join("，", parts) + "。";
    }

    private String playerAvailabilitySummary(PublicPlayerProfileSummary player) {
        ArrayList<String> parts = new ArrayList<>();
        addPart(parts, "伤停", player.injuryStatus());
        addPart(parts, "牌面", player.cardStatus());
        addPart(parts, "更衣室", player.lockerRoomStatus());
        return parts.isEmpty() ? "球员可用性字段来自正式球员表。" : String.join("，", parts) + "。";
    }

    private String playerLineupSummary(PlayerLineupParticipation participation) {
        StringBuilder summary = new StringBuilder("已关联 ")
                .append(participation.matchCount())
                .append(" 场比赛阵容记录");
        if (participation.starterCount() > 0) {
            summary.append("，其中首发 ")
                    .append(participation.starterCount())
                    .append(" 次");
        }
        ArrayList<String> latestParts = new ArrayList<>();
        if (participation.latestMatchday() != null) {
            latestParts.add(participation.latestMatchday().toString());
        }
        if (hasText(participation.latestMatchName())) {
            latestParts.add(participation.latestMatchName());
        }
        addPart(latestParts, "角色", participation.latestRole());
        addPart(latestParts, "位置", participation.latestPosition());
        if (!latestParts.isEmpty()) {
            summary.append("，最近记录 ")
                    .append(String.join("，", latestParts));
        }
        summary.append("。");
        return summary.toString();
    }

    private String playerEventSummary(PlayerEventParticipation participation) {
        StringBuilder summary = new StringBuilder("已关联 ")
                .append(participation.eventCount())
                .append(" 条比赛事件");
        if (participation.goalEventCount() > 0) {
            summary.append("，进球类 ")
                    .append(participation.goalEventCount())
                    .append(" 条");
        }
        if (participation.cardEventCount() > 0) {
            summary.append("，牌面 ")
                    .append(participation.cardEventCount())
                    .append(" 条");
        }
        ArrayList<String> latestParts = new ArrayList<>();
        if (participation.latestMatchday() != null) {
            latestParts.add(participation.latestMatchday().toString());
        }
        if (hasText(participation.latestMatchName())) {
            latestParts.add(participation.latestMatchName());
        }
        if (participation.latestMinute() != null) {
            latestParts.add("第 " + participation.latestMinute() + " 分钟");
        }
        addPart(latestParts, "事件", participation.latestEventType());
        if (!latestParts.isEmpty()) {
            summary.append("，最近记录 ")
                    .append(String.join("，", latestParts));
        }
        summary.append("。");
        return summary.toString();
    }

    private String playerMetricSummary(PublicPlayerMetricSummary metric) {
        ArrayList<String> parts = new ArrayList<>();
        if (metric.minutesPlayed() != null) {
            parts.add("出场分钟");
        }
        if (metric.xg() != null) {
            parts.add("xG");
        }
        if (metric.xa() != null) {
            parts.add("xA");
        }
        if (metric.shots() != null) {
            parts.add("射门");
        }
        if (metric.trainingLoad() != null) {
            parts.add("训练负荷");
        }
        if (metric.expectedStartingProbability() != null) {
            parts.add("预计首发概率");
        }
        return parts.isEmpty()
                ? "最新球员指标快照已入库。"
                : playerMetricHasAdvanced(metric)
                        ? "最新球员高阶/基础指标包含 " + String.join("、", parts) + "。"
                        : "最新球员比赛级基础指标包含 " + String.join("、", parts) + "；xG/xA、训练负荷和预计首发概率仍待补。";
    }

    private boolean teamMetricHasAnyPerformance(PublicTeamMetricSummary metric) {
        return metric != null && (metric.xg() != null
                || metric.xga() != null
                || metric.npxg() != null
                || metric.ppda() != null
                || metric.xpts() != null
                || metric.shots() != null
                || metric.shotsOnTarget() != null
                || metric.possessionPct() != null
                || metric.progressivePasses() != null
                || metric.setPieceXg() != null
                || metric.formScore() != null);
    }

    private boolean teamMetricHasAdvanced(PublicTeamMetricSummary metric) {
        return metric != null && (metric.xg() != null
                || metric.xga() != null
                || metric.npxg() != null
                || metric.ppda() != null
                || metric.xpts() != null
                || metric.progressivePasses() != null
                || metric.setPieceXg() != null
                || metric.formScore() != null);
    }

    private boolean playerMetricHasAnyPerformance(PublicPlayerMetricSummary metric) {
        return metric != null && (metric.minutesPlayed() != null
                || metric.goals() != null
                || metric.assists() != null
                || metric.xg() != null
                || metric.xa() != null
                || metric.npxg() != null
                || metric.shots() != null
                || metric.shotsOnTarget() != null
                || metric.keyPasses() != null
                || metric.progressivePasses() != null
                || metric.trainingLoad() != null
                || metric.availabilityScore() != null
                || metric.expectedStartingProbability() != null);
    }

    private boolean playerMetricHasAdvanced(PublicPlayerMetricSummary metric) {
        return metric != null && (metric.xg() != null
                || metric.xa() != null
                || metric.npxg() != null
                || metric.keyPasses() != null
                || metric.progressivePasses() != null
                || metric.trainingLoad() != null
                || metric.availabilityScore() != null
                || metric.expectedStartingProbability() != null);
    }

    private int distinctLineupMatchCount(List<PublicTeamLineup> lineups) {
        ArrayList<Long> matchIds = new ArrayList<>();
        for (PublicTeamLineup lineup : lineups) {
            if (lineup.matchId() != null && !matchIds.contains(lineup.matchId())) {
                matchIds.add(lineup.matchId());
            }
        }
        return matchIds.size();
    }

    private void addPart(List<String> parts, String label, String value) {
        if (hasText(value)) {
            parts.add(label + " " + value);
        }
    }

    private PublicProfileReadiness teamReadiness(
            PublicTeamProfileSummary team,
            List<PublicProfileFact> facts,
            List<PublicTeamPlayer> players,
            List<PublicTeamLineup> lineups,
            List<PublicTeamScoringPattern> scoringPatterns,
            List<PublicTeamExternalFactor> externalFactors,
            List<PublicTeamMatchHistory> matchHistory,
            long evidenceCount,
            long conflictCount,
            PublicTeamMetricSummary latestMetric
    ) {
        ArrayList<String> strengths = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        int passed = 0;
        int total = 10;

        passed += readinessCheck(hasText(team.countryIso2()) || hasText(team.flagAssetKey()) || hasText(team.fifaCode()), "国家/国旗元数据已覆盖", "缺国家 ISO/国旗/FIFA 编码", strengths, missing);
        passed += readinessCheck(hasText(team.attackProfile()), "已有进攻画像", "缺进攻结构化画像", strengths, missing);
        passed += readinessCheck(hasText(team.defenseProfile()), "已有防守画像", "缺防守结构化画像", strengths, missing);
        passed += readinessCheck(players.size() >= 18, "球员名单覆盖较好", "球员名单不足 18 人", strengths, missing);
        passed += readinessCheck(facts.size() >= 4, "画像事实达到基础覆盖", "画像事实少于 4 条", strengths, missing);
        passed += readinessCheck(!lineups.isEmpty(), "已有阵容/首发线索", "缺阵容或首发线索", strengths, missing);
        passed += readinessCheck(scoringPatterns.size() >= 3 || matchHistory.size() >= 3, "近期比赛趋势可读", "缺近三场进失球/历史趋势", strengths, missing);
        passed += readinessCheck(!externalFactors.isEmpty(), "已有外部因素线索", "缺天气、场地、裁判、赛程或舆情外因", strengths, missing);
        passed += readinessCheck(evidenceCount >= 3, "证据链达到基础核查量", "证据链少于 3 条", strengths, missing);
        passed += readinessCheck(teamMetricHasAnyPerformance(latestMetric), "已有比赛级技术指标", "缺射门、控球等比赛级技术指标", strengths, missing);
        if (!teamMetricHasAdvanced(latestMetric)) {
            missing.add("缺 xG、xGA、npxG、PPDA、xPTS 等专业高阶指标");
        }

        if (conflictCount > 0) {
            missing.add("存在 " + conflictCount + " 个冲突，结论需先核查");
        }

        int score = Math.max(0, Math.min(100, Math.round((passed * 100f) / total) - (conflictCount > 0 ? 8 : 0)));
        return readiness(score, strengths, missing, List.of(
                "优先补齐攻防画像、近三场趋势和关键球员可用性",
                "补 xG/xGA/PPDA、射门、控球、定位球等高阶指标",
                "用官方或第二来源核对冲突后再展示确定结论"
        ));
    }

    private PublicProfileReadiness playerReadiness(
            PublicPlayerProfileSummary player,
            List<PublicProfileFact> facts,
            PublicPlayerMetricSummary latestMetric
    ) {
        ArrayList<String> strengths = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        int passed = 0;
        int total = 9;

        passed += readinessCheck(player.team() != null && hasText(player.team().teamName()), "所属球队已绑定", "缺所属球队/国旗上下文", strengths, missing);
        passed += readinessCheck(player.shirtNumber() != null && hasText(player.position()), "号码和位置可读", "缺号码或位置", strengths, missing);
        passed += readinessCheck(hasText(player.status()), "基础状态已同步", "缺基础状态", strengths, missing);
        passed += readinessCheck(hasText(player.injuryStatus()), "伤病状态已同步", "缺伤病/恢复状态", strengths, missing);
        passed += readinessCheck(hasText(player.cardStatus()), "红黄牌状态已同步", "缺停赛/累计黄牌信息", strengths, missing);
        passed += readinessCheck(hasText(player.lockerRoomStatus()), "更衣室/士气状态已同步", "缺更衣室或士气线索", strengths, missing);
        passed += readinessCheck(facts.size() >= 3, "画像事实达到基础覆盖", "画像事实少于 3 条", strengths, missing);
        passed += readinessCheck(hasFactType(facts, "FORM") || hasFactType(facts, "TRAINING"), "已有状态/训练事实", "缺近期表现或训练事实", strengths, missing);
        passed += readinessCheck(playerMetricHasAnyPerformance(latestMetric), "已有比赛级表现指标", "缺出场、进球、助攻、射门等比赛级指标", strengths, missing);
        if (!playerMetricHasAdvanced(latestMetric)) {
            missing.add("缺 xG/xA、训练负荷、可用性评分或预计首发概率");
        }

        int score = Math.max(0, Math.min(100, Math.round((passed * 100f) / total)));
        return readiness(score, strengths, missing, List.of(
                "优先补伤病恢复、训练负荷、停赛风险和预计首发概率",
                "补近三场出场时间、xG/xA、射门、关键传球和定位球职责",
                "核心球员事实至少覆盖表现、训练、伤停、纪律和新闻来源"
        ));
    }

    private PublicProfileReadiness readiness(int score, List<String> strengths, List<String> missing, List<String> nextActions) {
        String level;
        String summary;
        if (score >= 80) {
            level = "GOOD";
            summary = "画像字段覆盖较完整，核心资料和缺失维度已展示。";
        } else if (score >= 55) {
            level = "PARTIAL";
            summary = "画像可用于初步了解，但仍有关键数据待补采。";
        } else {
            level = "WEAK";
            summary = "画像字段偏薄，事实、指标和来源缺口已列出。";
        }
        return new PublicProfileReadiness(score, level, summary, strengths, missing, nextActions);
    }

    private int readinessCheck(boolean condition, String strength, String missing, List<String> strengths, List<String> missingDimensions) {
        if (condition) {
            strengths.add(strength);
            return 1;
        }
        missingDimensions.add(missing);
        return 0;
    }

    private boolean hasFactType(List<PublicProfileFact> facts, String expectedType) {
        return facts.stream().anyMatch(fact -> expectedType.equalsIgnoreCase(fact.factType()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasAnyText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return true;
            }
        }
        return false;
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

    private record PlayerLineupParticipation(
            long matchCount,
            long starterCount,
            String latestMatchName,
            LocalDate latestMatchday,
            String latestRole,
            String latestPosition
    ) {
        static PlayerLineupParticipation empty() {
            return new PlayerLineupParticipation(0, 0, null, null, null, null);
        }
    }

    private record PlayerEventParticipation(
            long eventCount,
            long goalEventCount,
            long cardEventCount,
            String latestEventType,
            Integer latestMinute,
            String latestMatchName,
            LocalDate latestMatchday
    ) {
        static PlayerEventParticipation empty() {
            return new PlayerEventParticipation(0, 0, 0, null, null, null, null);
        }
    }
}
