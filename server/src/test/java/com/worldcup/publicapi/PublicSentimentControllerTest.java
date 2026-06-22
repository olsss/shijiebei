package com.worldcup.publicapi;

import com.worldcup.sentimentcenter.service.SentimentCenterQueryService;
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
class PublicSentimentControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    SentimentCenterQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void publicSentimentEndpointsUsePublicReadModelInsteadOfRichSentimentService() throws Exception {
        long matchId = createSentimentFixture();

        mockMvc.perform(get("/api/public/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/sentiment/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(matchId));
        mockMvc.perform(get("/api/public/sentiment/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("WEATHER"));
        mockMvc.perform(get("/api/public/sentiment/risk-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("PUBLIC_OVERHEAT"));

        verify(richQueryService, never()).overview();
        verify(richQueryService, never()).matchSentiment(anyLong());
        verify(richQueryService, never()).categories();
        verify(richQueryService, never()).riskTypes();
    }

    private long createSentimentFixture() {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("sentiment-home", "Sentiment Home");
        long awayTeamId = insertTeam("sentiment-away", "Sentiment Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);
        return matchId;
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "skill/archive/SECRET/job", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "SENTIMENT", "APPROVED", "sentiment.json", "4".repeat(64), "sentiment", true, "ok", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code) VALUES (?,?,?)", key, name, key.substring(0, 3).toUpperCase());
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, kickoff_time, home_team_id, away_team_id, status) VALUES (?,?,?,?,?,?,?,?,?,?)",
                "sentiment-home-away-20260623", "Sentiment Home vs Sentiment Away", "2026-06-23", "061", "World Cup", "Group",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "sentiment-home-away-20260623");
    }

    private long insertSentimentFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "Light rain", "summary reviewedBy=SECRET", "MIXED", "MATCH", "sentiment-home-away-20260623", "DATA_VENDOR",
                "Weather Provider", "source", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 10, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 6, 24, 10, 0)),
                "7.5", "8.0", "{\"category\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
    }

    private void insertSentimentRisk(long importItemId, long matchId, long factorId) {
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "PUBLIC_OVERHEAT", "HIGH", "78.0000", "Overheat", "rationale approvedBy=SECRET", "LOWER_CONFIDENCE", "Media Digest", "source", "{\"type\":\"SECRET\"}");
    }
}
