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
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
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

        mockMvc.perform(get("/api/public/profiles/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.id").value(fixture.teamId()));
        mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId() + "/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.playerId()));
        mockMvc.perform(get("/api/public/profiles/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.playerId()));
        mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.id").value(fixture.playerId()));

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
                .andExpect(jsonPath("$.data.team.factCount").value(1))
                .andExpect(jsonPath("$.data.facts.length()").value(1))
                .andExpect(jsonPath("$.data.facts[0].title").value("Team style"))
                .andExpect(content().string(not(containsString("Draft team fact"))))
                .andExpect(content().string(not(containsString("draft team summary"))));

        mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.factCount").value(1))
                .andExpect(jsonPath("$.data.facts.length()").value(1))
                .andExpect(jsonPath("$.data.facts[0].title").value("Player form"))
                .andExpect(content().string(not(containsString("Draft player fact"))))
                .andExpect(content().string(not(containsString("draft player summary"))));
    }

    private ProfileFixture createProfileFixture() {
        long teamId = insertTeam("profile-home", "Profile Home");
        long awayTeamId = insertTeam("profile-away", "Profile Away");
        long playerId = insertPlayer("profile-player", teamId, "Profile Player");
        long matchId = insertMatch(teamId, awayTeamId);
        insertLineup(matchId, teamId, playerId);
        insertTeamStats(matchId, teamId);
        insertTeamFact(teamId);
        insertPlayerFact(playerId);
        insertEvidence(matchId, "profile-home");
        insertConflict("profile-home");
        return new ProfileFixture(teamId, playerId);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "control", "attack", "defense", "positive", "{\"rawPayload\":\"SECRET\"}");
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

    private void insertPlayerFact(long playerId) {
        insertPlayerFact(playerId, "Player form", "summary approvedBy=SECRET", "admin");
    }

    private void insertPlayerFact(long playerId, String title, String summary, String approvedBy) {
        jdbcTemplate.update("INSERT INTO player_profile_facts(player_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                playerId, "FORM", title, summary, "test", "source", "8.0", approvedBy, "{\"raw\":\"SECRET\"}");
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
