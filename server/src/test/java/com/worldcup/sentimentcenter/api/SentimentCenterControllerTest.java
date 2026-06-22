package com.worldcup.sentimentcenter.api;

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
class SentimentCenterControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
        jdbcTemplate.update("DELETE FROM collection_items");
        jdbcTemplate.update("DELETE FROM collection_jobs");
        jdbcTemplate.update("DELETE FROM import_item_mappings");
        jdbcTemplate.update("DELETE FROM data_dictionaries");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM analysis_reports");
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
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
    void sentimentEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/sentiment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sentimentOverviewReturnsFactorsWithRiskSummary() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/sentiment").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("德国 vs 日本"))
                .andExpect(jsonPath("$.data[0].factorCategory").value("WEATHER"))
                .andExpect(jsonPath("$.data[0].factorType").value("RAIN"))
                .andExpect(jsonPath("$.data[0].title").value("预计小雨"))
                .andExpect(jsonPath("$.data[0].stale").value(true))
                .andExpect(jsonPath("$.data[0].riskCount").value(1))
                .andExpect(jsonPath("$.data[0].highestRiskLevel").value("HIGH"));
    }

    @Test
    void matchSentimentDetailReturnsFactorsAndRisks() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/sentiment/matches/" + fixture.matchId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data.matchName").value("德国 vs 日本"))
                .andExpect(jsonPath("$.data.factors[0].id").value(fixture.factorId()))
                .andExpect(jsonPath("$.data.factors[0].sourceName").value("Weather Provider"))
                .andExpect(jsonPath("$.data.risks[0].riskType").value("PUBLIC_OVERHEAT"))
                .andExpect(jsonPath("$.data.risks[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.risks[0].suggestedAction").value("LOWER_CONFIDENCE"));
    }

    @Test
    void categoriesAndRiskTypesReturnDistinctValues() throws Exception {
        createFixture();

        mockMvc.perform(get("/api/sentiment/categories").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("WEATHER"));

        mockMvc.perform(get("/api/sentiment/risk-types").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("PUBLIC_OVERHEAT"));
    }

    @Test
    void unknownMatchSentimentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/sentiment/matches/999999").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isNotFound());
    }

    private Fixture createFixture() {
        long importItemId = insertImportItem();
        long matchId = insertMatch();
        long factorId = insertFactor(importItemId, matchId);
        insertRisk(importItemId, matchId, factorId);
        return new Fixture(matchId, factorId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "test/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "SOURCE", "APPROVED", "source.json", "0".repeat(64), "舆情", true, "ok", "{}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertMatch() {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, status, result_status) VALUES (?,?,?,?,?,?)",
                "germany-japan-20260624", "德国 vs 日本", "2026-06-24", "周三001", "IMPORTED", "UNKNOWN");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "germany-japan-20260624");
    }

    private long insertFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, evidence_level, source_name, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "预计小雨", "赛前两小时有小雨", "MIXED", "MATCH", "DATA_VENDOR", "Weather Provider",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 20, 10, 0)),
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 21, 10, 0)),
                "7.5", "8.0", "{\"category\":\"WEATHER\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
    }

    private void insertRisk(long importItemId, long matchId, long factorId) {
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "PUBLIC_OVERHEAT", "HIGH", "78.0000", "舆论过热", "热门预期过度集中", "LOWER_CONFIDENCE", "Media Digest", "{\"type\":\"PUBLIC_OVERHEAT\"}");
    }

    private record Fixture(long matchId, long factorId) {
    }
}
