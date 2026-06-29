package com.worldcup.publicapi;

import com.worldcup.profile.service.ProfileQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicProfilesControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    ProfileQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM player_metric_snapshots");
        jdbcTemplate.update("DELETE FROM team_metric_snapshots");
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM data_conflicts");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
    }

    @Test
    void publicProfileEndpointsUsePublicReadModelInsteadOfRichProfileService() throws Exception {
        ProfileFixture fixture = createProfileFixture();
        insertGroupStandingFact(fixture.teamId());

        mockMvc.perform(get("/api/public/profiles/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.id").value(fixture.teamId()))
                .andExpect(jsonPath("$.data.team.countryIso2").value("CN"))
                .andExpect(jsonPath("$.data.team.flagAssetKey").value("cn"))
                .andExpect(jsonPath("$.data.team.confederation").value("AFC"))
                .andExpect(jsonPath("$.data.team.groupName").value("A组"))
                .andExpect(jsonPath("$.data.team.technicalMetricCount").value(1))
                .andExpect(jsonPath("$.data.team.advancedMetricCount").value(1))
                .andExpect(jsonPath("$.data.team.groupStandingRank").value(1))
                .andExpect(jsonPath("$.data.team.groupStandingPoints").value(7))
                .andExpect(jsonPath("$.data.team.groupStandingRecord").value("3场2胜1平0负"))
                .andExpect(jsonPath("$.data.team.groupGoalDifference").value(4))
                .andExpect(jsonPath("$.data.team.groupStandingSummary").value(containsString("A组第1/4名")))
                .andExpect(jsonPath("$.data.readiness.level").exists())
                .andExpect(jsonPath("$.data.readiness.missingDimensions[0]").exists())
                .andExpect(jsonPath("$.data.latestMetric.xg").value(1.8))
                .andExpect(jsonPath("$.data.latestMetric.ppda").value(9.2));
        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId() + "/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.playerId()));
        mockMvc.perform(get("/api/public/profiles/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.playerId()));
        mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.id").value(fixture.playerId()))
                .andExpect(jsonPath("$.data.player.team.countryIso2").value("CN"))
                .andExpect(jsonPath("$.data.player.performanceMetricCount").value(1))
                .andExpect(jsonPath("$.data.player.advancedMetricCount").value(1))
                .andExpect(jsonPath("$.data.readiness.score").exists())
                .andExpect(jsonPath("$.data.readiness.strengths[0]").exists())
                .andExpect(jsonPath("$.data.latestMetric.xg").value(0.7))
                .andExpect(jsonPath("$.data.latestMetric.expectedStartingProbability").value(0.82));

        verify(richQueryService, never()).teams();
        verify(richQueryService, never()).team(anyLong());
        verify(richQueryService, never()).teamPlayers(anyLong());
        verify(richQueryService, never()).players();
        verify(richQueryService, never()).player(anyLong());
    }

    @Test
    void publicProfileEndpointsExposeOnlyApprovedFacts() throws Exception {
        ProfileFixture fixture = createProfileFixture();
        insertTeamFact(fixture.teamId(), "Draft team fact", "draft team summary", null);
        insertPlayerFact(fixture.playerId(), "Draft player fact", "draft player summary", null);

        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.factCount").value(9))
                .andExpect(jsonPath("$.data.facts.length()").value(9))
                .andExpect(jsonPath("$.data.facts[0].title").value("Team style"))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("名单覆盖")))
                .andExpect(content().string(not(containsString("Draft team fact"))))
                .andExpect(content().string(not(containsString("draft team summary"))));

        mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.factCount").value(7))
                .andExpect(jsonPath("$.data.facts.length()").value(7))
                .andExpect(jsonPath("$.data.facts[0].title").value("Player form"))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("名单身份")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("阵容参与")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("比赛事件")))
                .andExpect(content().string(not(containsString("Draft player fact"))))
                .andExpect(content().string(not(containsString("draft player summary"))));
    }

    @Test
    void publicProfilesDeriveFactsFromFormalTablesWhenApprovedFactsAreEmpty() throws Exception {
        ProfileFixture fixture = createProfileFixtureWithoutApprovedFacts();

        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.factCount").value(7))
                .andExpect(jsonPath("$.data.facts.length()").value(7))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("国家队上下文")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("名单覆盖")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("近期比分")))
                .andExpect(jsonPath("$.data.facts[0].sourceName").value("正式库派生"));

        mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.factCount").value(5))
                .andExpect(jsonPath("$.data.facts.length()").value(5))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("国家队归属")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("名单身份")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("阵容参与")))
                .andExpect(jsonPath("$.data.facts[*].title", hasItem("比赛事件")))
                .andExpect(jsonPath("$.data.facts[0].sourceName").value("正式库派生"));
    }

    @Test
    void publicTeamProfileNormalizesJsonStyleTagsAndDerivesGroupName() throws Exception {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, raw_payload) VALUES (?,?,?,?,?)",
                "json-style-team", "JSON Style Team", "JST", "[\"世界杯2026参赛队\",\"K组\"]", "{\"rawPayload\":\"SECRET\"}");
        long teamId = jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, "json-style-team");

        mockMvc.perform(get("/api/public/profiles/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.styleTags").value("世界杯2026参赛队 · K组"))
                .andExpect(jsonPath("$.data.team.groupName").value("K组"))
                .andExpect(content().string(not(containsString("[\"世界杯2026参赛队\""))))
                .andExpect(content().string(not(containsString("rawPayload"))));
    }

    private ProfileFixture createProfileFixture() {
        long teamId = insertTeam("profile-home", "Profile Home");
        long awayTeamId = insertTeam("profile-away", "Profile Away");
        long playerId = insertPlayer("profile-player", teamId, "Profile Player");
        long matchId = insertMatch(teamId, awayTeamId);
        insertLineup(matchId, teamId, playerId);
        insertEvent(matchId, teamId, playerId, "GOAL", 66);
        insertTeamStats(matchId, teamId);
        insertTeamFact(teamId);
        insertPlayerFact(playerId);
        insertTeamMetric(teamId, matchId);
        insertPlayerMetric(playerId, teamId, matchId);
        insertEvidence(matchId, "profile-home");
        insertConflict("profile-home");
        return new ProfileFixture(teamId, playerId);
    }

    private ProfileFixture createProfileFixtureWithoutApprovedFacts() {
        long teamId = insertTeam("derived-home", "Derived Home");
        long awayTeamId = insertTeam("derived-away", "Derived Away");
        long playerId = insertPlayer("derived-player", teamId, "Derived Player");
        long matchId = insertMatch(teamId, awayTeamId);
        insertLineup(matchId, teamId, playerId);
        insertEvent(matchId, teamId, playerId, "YELLOW_CARD", 44);
        insertTeamStats(matchId, teamId);
        insertEvidence(matchId, "derived-home");
        return new ProfileFixture(teamId, playerId);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, country_region, country_iso2, flag_asset_key, confederation, group_name, metadata_source_ref, style_tags, attack_profile, defense_profile, public_sentiment, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "中国", "CN", "cn", "AFC", "A组", "FIFA teams metadata", "control", "attack", "defense", "positive", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                key, teamId, name, 9, "FW", "FIT", "none", "none", "stable", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "profile-home-away-20260623", "Profile Home vs Profile Away", "2026-06-23", "071", "World Cup", "Group",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "external approvedBy=SECRET", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "profile-home-away-20260623");
    }

    private void insertLineup(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, "STARTER", "FW", true);
    }

    private void insertEvent(long matchId, long teamId, long playerId, String eventType, int minute) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId, minute, eventType, teamId, playerId, "{\"event\":\"SECRET\"}");
    }

    private void insertTeamStats(long matchId, long teamId) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", 2, 1, 12, "12,77", "{\"shots\":\"SECRET\"}");
    }

    private void insertTeamFact(long teamId) {
        insertTeamFact(teamId, "Team style", "summary reviewedBy=SECRET", "admin");
    }

    private void insertTeamFact(long teamId, String title, String summary, String approvedBy) {
        jdbcTemplate.update("INSERT INTO team_profile_facts(team_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                teamId, "STYLE", title, summary, "test", "source", "8.0", approvedBy, "{\"raw\":\"SECRET\"}");
    }

    private void insertGroupStandingFact(long teamId) {
        jdbcTemplate.update("INSERT INTO team_profile_facts(team_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                teamId, "GROUP_STANDING_SNAPSHOT", "A组积分态势：第1名",
                "Profile Home当前A组第1/4名，3场2胜1平0负，进5球失1球，净胜球+4，积分7。该排名由正式库已完赛同组比赛比分派生。",
                "test", "derived:group-standing:test", "8.0", "admin", "{\"raw\":\"SECRET\"}");
    }

    private void insertPlayerFact(long playerId) {
        insertPlayerFact(playerId, "Player form", "summary approvedBy=SECRET", "admin");
    }

    private void insertPlayerFact(long playerId, String title, String summary, String approvedBy) {
        jdbcTemplate.update("INSERT INTO player_profile_facts(player_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                playerId, "FORM", title, summary, "test", "source", "8.0", approvedBy, "{\"raw\":\"SECRET\"}");
    }

    private void insertTeamMetric(long teamId, long matchId) {
        jdbcTemplate.update("INSERT INTO team_metric_snapshots(team_id, match_id, metric_type, xg, xga, npxg, ppda, xpts, shots, shots_on_target, possession_pct, progressive_passes, set_piece_xg, form_score, source_name, source_ref, captured_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                teamId, matchId, "RECENT", "1.8", "0.9", "1.5", "9.2", "2.1", 13, 6, "58.5", 41, "0.3", "76", "Scout", "team-metric", Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 19, 0)), "{\"raw\":\"SECRET\"}");
    }

    private void insertPlayerMetric(long playerId, long teamId, long matchId) {
        jdbcTemplate.update("INSERT INTO player_metric_snapshots(player_id, team_id, match_id, metric_type, minutes_played, goals, assists, xg, xa, npxg, shots, shots_on_target, key_passes, progressive_passes, training_load, availability_score, expected_starting_probability, source_name, source_ref, captured_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                playerId, teamId, matchId, "RECENT", 86, "1", "0", "0.7", "0.2", "0.6", 4, 2, 3, 8, "72", "88", "0.82", "Training", "player-metric", Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 19, 30)), "{\"raw\":\"SECRET\"}");
    }

    private void insertEvidence(long matchId, String sourceRef) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", sourceRef, Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 18, 0)),
                "official source ticketNo=SECRET", "9.5", "{\"official\":\"SECRET\"}");
    }

    private void insertConflict(String entityKey) {
        jdbcTemplate.update("INSERT INTO data_conflicts(conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                "PROFILE", entityKey, "rawPayload", "ticketNo=SECRET", "stake=88", "PENDING", "{\"field\":\"SECRET\"}");
    }

    private record ProfileFixture(long teamId, long playerId) {
    }
}
