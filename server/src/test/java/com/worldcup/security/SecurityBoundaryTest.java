package com.worldcup.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
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
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void publicGetNamespaceIsAnonymousButOnlyForReadMethods() throws Exception {
        mockMvc.perform(get("/api/public/not-yet-created"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/public/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/import-jobs/scan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyRichReadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/odds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/sentiment")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/prematch-workbench/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/teams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/core-data/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analysis-review/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/import-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/system/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadLegacyRichEndpoints() throws Exception {
        mockMvc.perform(get("/api/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/odds").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/sentiment").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/prematch-workbench/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/profiles/teams").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/core-data/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/analysis-review/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
    }

    @Test
    void publicEvidenceReadEndpointsAreAnonymousAndSanitized() throws Exception {
        PublicFixture fixture = createPublicFixture();

        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches/" + fixture.matchId())).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/lineups")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/events")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/team-stats")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/player-stats")).andExpect(status().isOk()));

        expectNoForbiddenFields(mockMvc.perform(get("/api/public/odds")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/odds/matches/" + fixture.matchId())).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/odds/bookmakers")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/odds/markets")).andExpect(status().isOk()));

        expectNoForbiddenFields(mockMvc.perform(get("/api/public/sentiment")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/sentiment/matches/" + fixture.matchId())).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/sentiment/categories")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/sentiment/risk-types")).andExpect(status().isOk()));

        expectNoForbiddenFields(mockMvc.perform(get("/api/public/profiles/teams")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId())).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/profiles/teams/" + fixture.teamId() + "/players")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/profiles/players")).andExpect(status().isOk()));
        expectNoForbiddenFields(mockMvc.perform(get("/api/public/profiles/players/" + fixture.playerId())).andExpect(status().isOk()));
    }

    private ResultActions expectNoForbiddenFields(ResultActions result) throws Exception {
        return result
                .andExpect(jsonPath("$..rawPayload").doesNotExist())
                .andExpect(jsonPath("$..payload").doesNotExist())
                .andExpect(jsonPath("$..approvedBy").doesNotExist())
                .andExpect(jsonPath("$..reviewedBy").doesNotExist())
                .andExpect(jsonPath("$..reviewNote").doesNotExist());
    }

    private PublicFixture createPublicFixture() {
        long importItemId = insertImportItem();
        long teamId = insertTeam("public-home", "Public Home");
        long awayTeamId = insertTeam("public-away", "Public Away");
        long playerId = insertPlayer("public-player", teamId, "Public Player");
        long matchId = insertMatch(teamId, awayTeamId);

        insertLineup(matchId, teamId, playerId);
        insertEvent(matchId, teamId, playerId);
        insertTeamStats(matchId, teamId);
        insertPlayerStats(matchId, playerId);
        insertEvidence(matchId);
        insertConflict(matchId);

        long marketId = insertOddsMarket(importItemId, matchId);
        insertOddsSelection(marketId, "HOME", "Home win");

        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);

        insertTeamFact(teamId);
        insertPlayerFact(playerId);
        return new PublicFixture(matchId, teamId, playerId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "test/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "PUBLIC", "APPROVED", "public.json", "0".repeat(64), "public", true, "ok", "{\"rawPayload\":true}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "control", "attack", "defense", "positive", "{\"rawPayload\":true}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key = ?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                key, teamId, name, 9, "FW", "FIT", "none", "none", "stable", "{\"rawPayload\":true}");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key = ?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "public-home-away-20260623", "Public Home vs Public Away", "2026-06-23", "031", "World Cup", "Group", "Test Stadium",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "weather=clear reviewedBy=admin", "{\"rawPayload\":true}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key = ?", Long.class, "public-home-away-20260623");
    }

    private void insertLineup(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, "STARTER", "FW", true);
    }

    private void insertEvent(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId, 12, "GOAL", teamId, playerId, "{\"assist\":\"corner\"}");
    }

    private void insertTeamStats(long matchId, long teamId) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", 2, 1, 12, "12,77", "{\"shots\":14}");
    }

    private void insertPlayerStats(long matchId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_player_stats(match_id, player_id, minutes_played, goals, assists, yellow_cards, red_cards, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, playerId, 90, 1, 1, 0, 0, "{\"xg\":0.7}");
    }

    private void insertEvidence(long matchId) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", "lineup", "https://example.test/fifa", Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 18, 0)),
                "official source ticketNo=SECRET", "9.5", "{\"official\":true}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "public-home-away-20260623", "lineup", "old", "new", "PENDING", "{\"field\":\"lineup\"}");
    }

    private long insertOddsMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "Pinnacle", "HAD", "Win Draw Win", "OPEN", null, null,
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)), "source reviewedBy=admin", "{\"market\":true}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM odds_market_snapshots", Long.class);
    }

    private void insertOddsSelection(long marketId, String code, String name) {
        jdbcTemplate.update("INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                marketId, code, name, "1.8000", "0.555556", "ACTIVE", "{\"selection\":true}");
    }

    private long insertSentimentFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "Light rain", "summary reviewedBy=admin", "MIXED", "MATCH", "public-home-away-20260623", "DATA_VENDOR",
                "Weather Provider", "source", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 10, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 6, 24, 10, 0)),
                "7.5", "8.0", "{\"category\":\"WEATHER\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
    }

    private void insertSentimentRisk(long importItemId, long matchId, long factorId) {
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "PUBLIC_OVERHEAT", "HIGH", "78.0000", "Overheat", "rationale approvedBy=admin", "LOWER_CONFIDENCE", "Media Digest", "source", "{\"type\":\"PUBLIC_OVERHEAT\"}");
    }

    private void insertTeamFact(long teamId) {
        jdbcTemplate.update("INSERT INTO team_profile_facts(team_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                teamId, "STYLE", "Team style", "summary reviewedBy=admin", "test", "source", "8.0", "admin", "{\"raw\":true}");
    }

    private void insertPlayerFact(long playerId) {
        jdbcTemplate.update("INSERT INTO player_profile_facts(player_id, fact_type, title, summary, source_name, source_ref, reliability_score, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                playerId, "FORM", "Player form", "summary approvedBy=admin", "test", "source", "8.0", "admin", "{\"raw\":true}");
    }

    private record PublicFixture(long matchId, long teamId, long playerId) {
    }
}
