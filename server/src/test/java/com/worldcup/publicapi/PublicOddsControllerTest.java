package com.worldcup.publicapi;

import com.worldcup.oddscenter.service.OddsCenterQueryService;
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
import org.springframework.test.web.servlet.ResultActions;

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
class PublicOddsControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    OddsCenterQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void matchOddsUsesPublicReadModelWithoutRawPayloadOrRichQueryService() throws Exception {
        long matchId = createOddsFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/odds/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(matchId))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("Odds Home"))
                .andExpect(jsonPath("$.data.homeTeam.countryIso2").value("CN"))
                .andExpect(jsonPath("$.data.awayTeam.teamName").value("Odds Away"))
                .andExpect(jsonPath("$.data.scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data.scoreboard.winnerSide").value("HOME"))
                .andExpect(jsonPath("$.data.scoreboard.scoreSource").value("TEAM_STATS"))
                .andExpect(jsonPath("$.data.markets[0].bookmaker").value("Pinnacle"))
                .andExpect(jsonPath("$.data.markets[0].selections[0].selectionCode").value("HOME"))
                .andExpect(jsonPath("$..rawPayload").doesNotExist())
                .andExpect(jsonPath("$..payload").doesNotExist()));

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/odds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].homeTeam.teamName").value("Odds Home"))
                .andExpect(jsonPath("$.data[0].awayTeam.teamName").value("Odds Away"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("2 - 1")));

        verify(richQueryService, never()).matchOdds(anyLong());
    }

    @Test
    void matchOddsFallbackToGoalEventsWhenTeamStatsAreMissing() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("event-odds-home", "Odds Event Home");
        long awayTeamId = insertTeam("event-odds-away", "Odds Event Away");
        long matchId = insertMatch(homeTeamId, awayTeamId, "event-odds-home-away-20260624", "Odds Event Home vs Odds Event Away");
        insertEvent(matchId, homeTeamId, "GOAL");
        insertEvent(matchId, awayTeamId, "PENALTY_GOAL");
        insertEvent(matchId, awayTeamId, "GOAL_FREE_KICK");
        insertEvent(matchId, homeTeamId, "RED_CARD");
        long marketId = insertOddsMarket(importItemId, matchId);
        insertOddsSelection(marketId);

        mockMvc.perform(get("/api/public/odds/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreboard.scoreDisplay").value("1 - 2"))
                .andExpect(jsonPath("$.data.scoreboard.winnerSide").value("AWAY"))
                .andExpect(jsonPath("$.data.scoreboard.scoreSource").value("MATCH_EVENTS"));

        mockMvc.perform(get("/api/public/odds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("1 - 2"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreSource").value("MATCH_EVENTS"));
    }

    private ResultActions expectNoForbiddenFieldsOrTokens(ResultActions result) throws Exception {
        return result
                .andExpect(content().string(not(containsString("rawPayload"))))
                .andExpect(content().string(not(containsString("payload"))))
                .andExpect(content().string(not(containsString("approvedBy"))))
                .andExpect(content().string(not(containsString("reviewedBy"))))
                .andExpect(content().string(not(containsString("reviewNote"))))
                .andExpect(content().string(not(containsString("SECRET"))));
    }

    private long createOddsFixture() {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("odds-home", "Odds Home");
        long awayTeamId = insertTeam("odds-away", "Odds Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertTeamStats(matchId, homeTeamId, 2, 1);
        insertTeamStats(matchId, awayTeamId, 1, 2);
        long marketId = insertOddsMarket(importItemId, matchId);
        insertOddsSelection(marketId);
        return matchId;
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "skill/archive/SECRET/job", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "ODDS", "APPROVED", "odds.json", "3".repeat(64), "odds", true, "ok", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, country_region, country_iso2, flag_asset_key, raw_payload) VALUES (?,?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "中国", "CN", "cn", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        return insertMatch(homeTeamId, awayTeamId, "odds-home-away-20260623", "Odds Home vs Odds Away");
    }

    private long insertMatch(long homeTeamId, long awayTeamId, String matchKey, String matchName) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, kickoff_time, home_team_id, away_team_id, status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                matchKey, matchName, "2026-06-23", "041", "World Cup", "Group",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED",
                "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, matchKey);
    }

    private void insertTeamStats(long matchId, long teamId, int goalsFor, int goalsAgainst) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", goalsFor, goalsAgainst, goalsFor > 1 ? "12,77" : "83", "{\"stats\":\"SECRET\"}");
    }

    private void insertEvent(long matchId, long teamId, String eventType) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, team_id, event_minute, event_type, payload) VALUES (?,?,?,?,?)",
                matchId, teamId, 20, eventType, "{\"event\":\"SECRET\"}");
    }

    private long insertOddsMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "Pinnacle", "HAD", "Win Draw Win", "OPEN", null, null,
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)), "source reviewedBy=SECRET", "{\"market\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM odds_market_snapshots", Long.class);
    }

    private void insertOddsSelection(long marketId) {
        jdbcTemplate.update("INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                marketId, "HOME", "Home win", "1.8000", "0.555556", "ACTIVE", "{\"selection\":\"SECRET\"}");
    }
}
