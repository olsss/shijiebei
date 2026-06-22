package com.worldcup.matchcenter.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchCenterControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
        jdbcTemplate.update("DELETE FROM collection_items");
        jdbcTemplate.update("DELETE FROM collection_jobs");
        jdbcTemplate.update("DELETE FROM import_item_mappings");
        jdbcTemplate.update("DELETE FROM data_dictionaries");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM analysis_reports");
        jdbcTemplate.update("DELETE FROM odds_snapshots");
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
    void matchEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchListReturnsScheduleContextAndCompletenessCounts() throws Exception {
        MatchFixture fixture = createFixture();

        mockMvc.perform(get("/api/matches").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchKey").value("spain-brazil-20260623"))
                .andExpect(jsonPath("$.data[0].matchName").value("西班牙 vs 巴西"))
                .andExpect(jsonPath("$.data[0].matchday").value("2026-06-23"))
                .andExpect(jsonPath("$.data[0].jcCode").value("031"))
                .andExpect(jsonPath("$.data[0].competition").value("世界杯"))
                .andExpect(jsonPath("$.data[0].stage").value("小组赛"))
                .andExpect(jsonPath("$.data[0].venue").value("洛杉矶"))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].resultStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].homeTeamId").value(fixture.homeTeamId()))
                .andExpect(jsonPath("$.data[0].homeTeamName").value("西班牙"))
                .andExpect(jsonPath("$.data[0].awayTeamId").value(fixture.awayTeamId()))
                .andExpect(jsonPath("$.data[0].awayTeamName").value("巴西"))
                .andExpect(jsonPath("$.data[0].eventCount").value(1))
                .andExpect(jsonPath("$.data[0].lineupCount").value(2))
                .andExpect(jsonPath("$.data[0].evidenceCount").value(1))
                .andExpect(jsonPath("$.data[0].conflictCount").value(1));
    }

    @Test
    void matchDetailAggregatesAllContextRows() throws Exception {
        MatchFixture fixture = createFixture();

        mockMvc.perform(get("/api/matches/" + fixture.matchId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.matchName").value("西班牙 vs 巴西"))
                .andExpect(jsonPath("$.data.externalFactors").value("{\"weather\":\"高温\",\"sentiment\":\"主队舆情稳定\"}"))
                .andExpect(jsonPath("$.data.lineups[0].playerName").value("莫拉塔"))
                .andExpect(jsonPath("$.data.lineups[0].starter").value(true))
                .andExpect(jsonPath("$.data.events[0].eventMinute").value(12))
                .andExpect(jsonPath("$.data.events[0].eventType").value("GOAL"))
                .andExpect(jsonPath("$.data.events[0].playerName").value("莫拉塔"))
                .andExpect(jsonPath("$.data.teamStats[0].teamName").value("西班牙"))
                .andExpect(jsonPath("$.data.teamStats[0].firstGoalMinute").value(12))
                .andExpect(jsonPath("$.data.teamStats[0].scoringMinutes").value("12,77"))
                .andExpect(jsonPath("$.data.playerStats[0].playerName").value("莫拉塔"))
                .andExpect(jsonPath("$.data.playerStats[0].teamName").value("西班牙"))
                .andExpect(jsonPath("$.data.playerStats[0].goals").value(1))
                .andExpect(jsonPath("$.data.playerStats[0].yellowCards").value(0))
                .andExpect(jsonPath("$.data.evidence[0].sourceName").value("FIFA"))
                .andExpect(jsonPath("$.data.evidence[0].reliabilityScore").value(9.5))
                .andExpect(jsonPath("$.data.conflicts[0].fieldName").value("lineup"))
                .andExpect(jsonPath("$.data.conflicts[0].resolutionStatus").value("PENDING"));
    }

    @Test
    void matchSubresourcesReturnScopedRows() throws Exception {
        MatchFixture fixture = createFixture();

        mockMvc.perform(get("/api/matches/" + fixture.matchId() + "/lineups").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamName").value("西班牙"))
                .andExpect(jsonPath("$.data[0].starter").value(true));

        mockMvc.perform(get("/api/matches/" + fixture.matchId() + "/events").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("GOAL"));

        mockMvc.perform(get("/api/matches/" + fixture.matchId() + "/team-stats").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].goalsFor").value(2));

        mockMvc.perform(get("/api/matches/" + fixture.matchId() + "/player-stats").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].assists").value(1));
    }

    @Test
    void unknownMatchReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/matches/999999").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isNotFound());
    }

    private MatchFixture createFixture() {
        long homeTeamId = insertTeam("spain", "西班牙");
        long awayTeamId = insertTeam("brazil", "巴西");
        long homePlayerId = insertPlayer("morata", homeTeamId, "莫拉塔", 7, "ST");
        long awayPlayerId = insertPlayer("vinicius", awayTeamId, "维尼修斯", 11, "LW");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertLineup(matchId, homeTeamId, homePlayerId, "ST", true);
        insertLineup(matchId, awayTeamId, awayPlayerId, "LW", true);
        insertEvent(matchId, homeTeamId, homePlayerId);
        insertTeamStats(matchId, homeTeamId, 2, 1, 12, "12,77", "{\"shots\":14}");
        insertTeamStats(matchId, awayTeamId, 1, 2, 55, "55", "{\"shots\":9}");
        insertPlayerStats(matchId, homePlayerId, 90, 1, 1, 0, 0, "{\"xg\":0.7}");
        insertEvidence(matchId);
        insertConflict(matchId);
        return new MatchFixture(matchId, homeTeamId, awayTeamId);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment) VALUES (?,?,?,?,?,?,?)",
                key, name, key.substring(0, Math.min(3, key.length())).toUpperCase(), "传控", "渗透", "压迫", "稳定");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key = ?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name, int shirtNumber, String position) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status) VALUES (?,?,?,?,?,?,?,?,?)",
                key, teamId, name, shirtNumber, position, "FIT", "无", "无", "稳定");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key = ?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "spain-brazil-20260623", "西班牙 vs 巴西", "2026-06-23", "031", "世界杯", "小组赛", "洛杉矶",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 3, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "{\"weather\":\"高温\",\"sentiment\":\"主队舆情稳定\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key = ?", Long.class, "spain-brazil-20260623");
    }

    private void insertLineup(long matchId, long teamId, long playerId, String position, boolean starter) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, starter ? "STARTER" : "BENCH", position, starter);
    }

    private void insertEvent(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId, 12, "GOAL", teamId, playerId, "{\"assist\":\"corner\"}");
    }

    private void insertTeamStats(long matchId, long teamId, int goalsFor, int goalsAgainst, int firstGoalMinute, String scoringMinutes, String payload) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", goalsFor, goalsAgainst, firstGoalMinute, scoringMinutes, payload);
    }

    private void insertPlayerStats(long matchId, long playerId, int minutesPlayed, int goals, int assists, int yellowCards, int redCards, String payload) {
        jdbcTemplate.update("INSERT INTO match_player_stats(match_id, player_id, minutes_played, goals, assists, yellow_cards, red_cards, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, playerId, minutesPlayed, goals, assists, yellowCards, redCards, payload);
    }

    private void insertEvidence(long matchId) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", "lineup", "https://example.test/fifa", Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 2, 0)), "官方首发名单", "9.5", "{\"official\":true}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "spain-brazil-20260623", "lineup", "旧首发", "新首发", "PENDING", "{\"field\":\"lineup\"}");
    }

    private record MatchFixture(long matchId, long homeTeamId, long awayTeamId) {
    }
}
