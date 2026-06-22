package com.worldcup.oddscenter.api;

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
class OddsCenterControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
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
    void oddsEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/odds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oddsOverviewReturnsMarketSummaries() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/odds").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("法国 vs 巴西"))
                .andExpect(jsonPath("$.data[0].bookmaker").value("Pinnacle"))
                .andExpect(jsonPath("$.data[0].marketCode").value("HAD"))
                .andExpect(jsonPath("$.data[0].marketName").value("胜平负"))
                .andExpect(jsonPath("$.data[0].snapshotType").value("OPEN"))
                .andExpect(jsonPath("$.data[0].selectionCount").value(3));
    }

    @Test
    void matchOddsDetailReturnsMarketsAndSelections() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/odds/matches/" + fixture.matchId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data.matchName").value("法国 vs 巴西"))
                .andExpect(jsonPath("$.data.markets[0].id").value(fixture.marketId()))
                .andExpect(jsonPath("$.data.markets[0].selections[0].selectionCode").value("HOME"))
                .andExpect(jsonPath("$.data.markets[0].selections[0].selectionName").value("主胜"))
                .andExpect(jsonPath("$.data.markets[0].selections[0].oddsValue").value(1.8))
                .andExpect(jsonPath("$.data.markets[0].selections[2].selectionCode").value("AWAY"));
    }

    @Test
    void bookmakersAndMarketsReturnDistinctValues() throws Exception {
        createFixture();

        mockMvc.perform(get("/api/odds/bookmakers").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("Pinnacle"));

        mockMvc.perform(get("/api/odds/markets").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].marketCode").value("HAD"))
                .andExpect(jsonPath("$.data[0].marketName").value("胜平负"));
    }

    @Test
    void unknownMatchOddsReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/odds/matches/999999").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isNotFound());
    }

    private Fixture createFixture() {
        long importItemId = insertImportItem();
        long matchId = insertMatch();
        long marketId = insertMarket(importItemId, matchId);
        insertSelection(marketId, "HOME", "主胜", "1.80");
        insertSelection(marketId, "DRAW", "平", "3.40");
        insertSelection(marketId, "AWAY", "客胜", "4.20");
        return new Fixture(matchId, marketId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "test/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "ODDS", "APPROVED", "odds.json", "0".repeat(64), "赔率", true, "ok", "{}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertMatch() {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, status, result_status) VALUES (?,?,?,?,?,?)",
                "france-brazil-20260623", "法国 vs 巴西", "2026-06-23", "周一001", "IMPORTED", "UNKNOWN");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "france-brazil-20260623");
    }

    private long insertMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, handicap_line, line_value, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "Pinnacle", "HAD", "胜平负", "OPEN", null, null,
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 10, 0)), "ref-1", "{\"market\":\"HAD\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM odds_market_snapshots", Long.class);
    }

    private void insertSelection(long marketId, String code, String name, String oddsValue) {
        jdbcTemplate.update("INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, selection_status, raw_payload) VALUES (?,?,?,?,?,?)",
                marketId, code, name, oddsValue, "OPEN", "{\"code\":\"" + code + "\"}");
    }

    private record Fixture(long matchId, long marketId) {
    }
}
