package com.worldcup.publicapi;

import com.worldcup.matchcenter.service.MatchCenterQueryService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicMatchesControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    MatchCenterQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM data_conflicts");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_player_stats");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
    }

    @Test
    void publicMatchEndpointsUsePublicReadModelInsteadOfRichMatchService() throws Exception {
        MatchFixture fixture = createMatchFixture();

        mockMvc.perform(get("/api/public/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.matchId()));
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.id").value(fixture.matchId()));
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/team-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/player-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());

        verify(richQueryService, never()).matches();
        verify(richQueryService, never()).match(anyLong());
        verify(richQueryService, never()).lineups(anyLong());
        verify(richQueryService, never()).events(anyLong());
        verify(richQueryService, never()).teamStats(anyLong());
        verify(richQueryService, never()).playerStats(anyLong());
    }

    private MatchFixture createMatchFixture() {
        long homeTeamId = insertTeam("match-home", "Match Home");
        long awayTeamId = insertTeam("match-away", "Match Away");
        long playerId = insertPlayer("match-player", homeTeamId, "Match Player");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertLineup(matchId, homeTeamId, playerId);
        insertEvent(matchId, homeTeamId, playerId);
        insertTeamStats(matchId, homeTeamId);
        insertPlayerStats(matchId, playerId);
        insertEvidence(matchId);
        insertConflict(matchId);
        return new MatchFixture(matchId);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, raw_payload) VALUES (?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, raw_payload) VALUES (?,?,?,?,?,?)",
                key, teamId, name, 9, "FW", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "match-home-away-20260623", "Match Home vs Match Away", "2026-06-23", "051", "World Cup", "Group", "Test Stadium",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "weather approvedBy=SECRET", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "match-home-away-20260623");
    }

    private void insertLineup(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, "STARTER", "FW", true);
    }

    private void insertEvent(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId, 12, "GOAL", teamId, playerId, "{\"payload\":\"SECRET\"}");
    }

    private void insertTeamStats(long matchId, long teamId) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", 2, 1, 12, "12,77", "{\"shots\":\"SECRET\"}");
    }

    private void insertPlayerStats(long matchId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_player_stats(match_id, player_id, minutes_played, goals, assists, yellow_cards, red_cards, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, playerId, 90, 1, 1, 0, 0, "{\"xg\":\"SECRET\"}");
    }

    private void insertEvidence(long matchId) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", "lineup", "https://example.test/fifa",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 18, 0)),
                "official source ticketNo=SECRET", "9.5", "{\"official\":\"SECRET\"}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "match-home-away-20260623", "payload.ticketNo", "ticketNo=SECRET", "stake=88", "PENDING", "{\"field\":\"SECRET\"}");
    }

    private record MatchFixture(long matchId) {
    }
}
