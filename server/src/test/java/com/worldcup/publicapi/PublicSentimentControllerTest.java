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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM match_team_stats");
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
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].homeTeam.teamName").value("Sentiment Home"))
                .andExpect(jsonPath("$.data[0].homeTeam.countryIso2").value("SH"))
                .andExpect(jsonPath("$.data[0].awayTeam.teamName").value("Sentiment Away"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("1 - 1"))
                .andExpect(jsonPath("$.data[0].scoreboard.winnerSide").value("DRAW"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreSource").value("TEAM_STATS"))
                .andExpect(jsonPath("$.data[*].factorCategory", hasItem("SCHEDULE")))
                .andExpect(jsonPath("$.data[*].factorCategory", hasItem("MARKET_SIGNAL")));
        mockMvc.perform(get("/api/public/sentiment/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(matchId))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("Sentiment Home"))
                .andExpect(jsonPath("$.data.awayTeam.countryIso2").value("SA"))
                .andExpect(jsonPath("$.data.scoreboard.scoreDisplay").value("1 - 1"))
                .andExpect(jsonPath("$.data.factors[0].summary").value("summary [REDACTED]"))
                .andExpect(jsonPath("$.data.factors[*].factorCategory", hasItem("SCHEDULE")))
                .andExpect(jsonPath("$.data.factors[*].factorCategory", hasItem("MARKET_SIGNAL")))
                .andExpect(jsonPath("$.data.factors[0].rawPayload").doesNotExist())
                .andExpect(jsonPath("$.data.risks[0].rawPayload").doesNotExist());
        mockMvc.perform(get("/api/public/sentiment/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("WEATHER")))
                .andExpect(jsonPath("$.data", hasItem("SCHEDULE")))
                .andExpect(jsonPath("$.data", hasItem("MARKET_SIGNAL")));
        mockMvc.perform(get("/api/public/sentiment/risk-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("PUBLIC_OVERHEAT"));

        verify(richQueryService, never()).overview();
        verify(richQueryService, never()).matchSentiment(anyLong());
        verify(richQueryService, never()).categories();
        verify(richQueryService, never()).riskTypes();
    }


    @Test
    void publicSentimentSuppressesDynamicOddsCardsWhenFormalMarketSnapshotExists() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("market-home", "Market Home");
        long awayTeamId = insertTeam("market-away", "Market Away");
        long matchId = insertMatch(homeTeamId, awayTeamId, "market-home-away-20260629", "Market Home vs Market Away");
        insertOddsMarket(importItemId, matchId);
        insertFormalMarketSnapshotFactor(importItemId, matchId);

        mockMvc.perform(get("/api/public/sentiment/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factors[*].factorType", hasItem("MARKET_PRICE_SNAPSHOT")))
                .andExpect(jsonPath("$.data.factors[*].factorType", not(hasItem("H2H_1X2"))));

        mockMvc.perform(get("/api/public/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].factorType", hasItem("MARKET_PRICE_SNAPSHOT")))
                .andExpect(jsonPath("$.data[*].factorType", not(hasItem("H2H_1X2"))));
    }

    @Test
    void publicSentimentFallbackToGoalEventsWhenTeamStatsAreMissing() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("event-sentiment-home", "Sentiment Event Home");
        long awayTeamId = insertTeam("event-sentiment-away", "Sentiment Event Away");
        long matchId = insertMatch(homeTeamId, awayTeamId, "event-sentiment-home-away-20260624", "Sentiment Event Home vs Sentiment Event Away");
        insertEvent(matchId, homeTeamId, "GOAL_HEADER");
        insertEvent(matchId, homeTeamId, "PENALTY_SCORED");
        insertEvent(matchId, awayTeamId, "GOAL");
        insertEvent(matchId, awayTeamId, "YELLOW_CARD");
        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);

        mockMvc.perform(get("/api/public/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data[0].scoreboard.winnerSide").value("HOME"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreSource").value("MATCH_EVENTS"));

        mockMvc.perform(get("/api/public/sentiment/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data.scoreboard.scoreSource").value("MATCH_EVENTS"));
    }

    @Test
    void publicSentimentUsesMostReliableEvidenceSummaryForScoreFallback() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("evidence-score-home", "Sentiment Evidence Home");
        long awayTeamId = insertTeam("evidence-score-away", "Sentiment Evidence Away");
        long matchId = insertMatch(homeTeamId, awayTeamId, "evidence-score-home-away-20260625", "Sentiment Evidence Home vs Sentiment Evidence Away");
        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);
        insertSourceEvidence(matchId, "赛后资料摘要，没有比分字段", "3.00", LocalDateTime.of(2026, 6, 25, 12, 0));
        insertSourceEvidence(matchId, "官方比分 2 - 0，主队获胜", "9.50", LocalDateTime.of(2026, 6, 25, 10, 0));

        mockMvc.perform(get("/api/public/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("2 - 0"))
                .andExpect(jsonPath("$.data[0].scoreboard.winnerSide").value("HOME"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreSource").value("EVIDENCE_TEXT"));

        mockMvc.perform(get("/api/public/sentiment/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreboard.scoreDisplay").value("2 - 0"))
                .andExpect(jsonPath("$.data.scoreboard.scoreSource").value("EVIDENCE_TEXT"));
    }

    private long createSentimentFixture() {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("sentiment-home", "Sentiment Home");
        long awayTeamId = insertTeam("sentiment-away", "Sentiment Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertTeamStats(matchId, homeTeamId, awayTeamId);
        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);
        insertOddsMarket(importItemId, matchId);
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
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, country_iso2, flag_asset_key, country_region) VALUES (?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), key.endsWith("home") ? "SH" : "SA", key.substring(0, 3).toUpperCase(), "测试国家");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        return insertMatch(homeTeamId, awayTeamId, "sentiment-home-away-20260623", "Sentiment Home vs Sentiment Away");
    }

    private long insertMatch(long homeTeamId, long awayTeamId, String matchKey, String matchName) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, kickoff_time, home_team_id, away_team_id, status) VALUES (?,?,?,?,?,?,?,?,?,?)",
                matchKey, matchName, "2026-06-23", "061", "World Cup", "Group",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, matchKey);
    }

    private void insertTeamStats(long matchId, long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, scoring_minutes) VALUES (?,?,?,?,?,?)",
                matchId, homeTeamId, "FULL_TIME", 1, 1, "12");
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, scoring_minutes) VALUES (?,?,?,?,?,?)",
                matchId, awayTeamId, "FULL_TIME", 1, 1, "67");
    }

    private void insertEvent(long matchId, long teamId, String eventType) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, team_id, event_minute, event_type, payload) VALUES (?,?,?,?,?)",
                matchId, teamId, 24, eventType, "{\"event\":\"SECRET\"}");
    }

    private long insertSentimentFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "Light rain", "summary reviewedBy=SECRET", "MIXED", "MATCH", "sentiment-home-away-20260623", "DATA_VENDOR",
                "Weather Provider", "source", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 10, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 6, 24, 10, 0)),
                "7.5", "8.0", "{\"category\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
    }

    private void insertOddsMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, line_value, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "DraftKings", "H2H_1X2", "Full Time Result / 1X2", "PRE_MATCH", "home/draw/away",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)), "odds-source", "{\"market\":\"SECRET\"}");
    }

    private void insertFormalMarketSnapshotFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "MARKET_SIGNAL", "MARKET_PRICE_SNAPSHOT", "赛前市场价格快照",
                "单点赔率市场快照，仅展示市场价格与隐含概率。", "NEUTRAL", "MATCH", "market-home-away-20260629", "STRUCTURED_API",
                "ESPN Summary API / DraftKings odds", "derived:market-price-snapshot:test", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 6, 29, 20, 0)),
                "7.2", "7.2", "{\"market\":\"SECRET\"}");
    }

    private void insertSourceEvidence(long matchId, String summary, String reliabilityScore, LocalDateTime evidenceTime) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "Official Score Source", "score-source", "https://example.test/score",
                Timestamp.valueOf(evidenceTime), summary, reliabilityScore, "{\"score\":\"SECRET\"}");
    }

    private void insertSentimentRisk(long importItemId, long matchId, long factorId) {
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "PUBLIC_OVERHEAT", "HIGH", "78.0000", "Overheat", "rationale approvedBy=SECRET", "LOWER_CONFIDENCE", "Media Digest", "source", "{\"type\":\"SECRET\"}");
    }
}
