package com.worldcup.publicapi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.worldcup.prematchworkbench.service.PrematchWorkbenchQueryService;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
class PublicPrematchWorkbenchControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    PrematchWorkbenchQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
        jdbcTemplate.update("DELETE FROM review_lessons");
        jdbcTemplate.update("DELETE FROM post_match_reviews");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM bet_plan_items");
        jdbcTemplate.update("DELETE FROM bet_plans");
        jdbcTemplate.update("DELETE FROM analysis_reports");
        jdbcTemplate.update("DELETE FROM data_conflicts");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_player_stats");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void matchesArePublicSummariesOnlyAndUseWorkbenchData() throws Exception {
        PrematchFixture fixture = createPrematchFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/prematch-workbench/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("Public Home vs Public Away")));
    }

    @Test
    void matchesUseDefaultPublicLimit() throws Exception {
        long homeTeamId = insertTeam("limit-home", "Limit Home");
        long awayTeamId = insertTeam("limit-away", "Limit Away");
        for (int i = 0; i < 55; i++) {
            insertMatch("limit-match-" + i, "Limit Home vs Limit Away " + i, homeTeamId, awayTeamId);
        }

        mockMvc.perform(get("/api/public/prematch-workbench/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(50)));
    }

    @Test
    void matchDetailIsPublicAndDropsBettingDetailsAndRawNarrative() throws Exception {
        PrematchFixture fixture = createPrematchFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/prematch-workbench/matches/" + fixture.matchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data.teams").isArray())
                .andExpect(jsonPath("$.data.lineups").isArray())
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.data.oddsMarkets").isArray())
                .andExpect(jsonPath("$.data.sentimentFactors").isArray())
                .andExpect(jsonPath("$.data.evidence").isArray())
                .andExpect(jsonPath("$.data.conflicts").isArray())
                .andExpect(jsonPath("$.data.analysisReports").isArray())
                .andExpect(jsonPath("$.data.integrityChecks").isArray())
                .andExpect(jsonPath("$.data.betPlans").doesNotExist())
                .andExpect(jsonPath("$.data.bets").doesNotExist())
                .andExpect(jsonPath("$..narrativeMd").doesNotExist()));

        verify(richQueryService, never()).match(anyLong());
    }

    @Test
    void matchDetailAndSummaryUseOnlyApprovedProfileFacts() throws Exception {
        PrematchFixture fixture = createPrematchFixture();
        Long homeTeamId = jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, "prematch-home");
        Long playerId = jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key=?", Long.class, "prematch-player");
        insertTeamFact(homeTeamId, "Draft team fact", "draft team summary", null);
        insertPlayerFact(playerId, "Draft player fact", "draft player summary", null);

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/prematch-workbench/matches"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].teamProfileCount").value(2))
                        .andExpect(jsonPath("$.data[0].playerProfileCount").value(1)))
                .andExpect(content().string(not(containsString("Draft team fact"))))
                .andExpect(content().string(not(containsString("Draft player fact"))));

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/prematch-workbench/matches/" + fixture.matchId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.summary.teamProfileCount").value(2))
                        .andExpect(jsonPath("$.data.summary.playerProfileCount").value(1)))
                .andExpect(content().string(not(containsString("Draft team fact"))))
                .andExpect(content().string(not(containsString("draft team summary"))))
                .andExpect(content().string(not(containsString("Draft player fact"))))
                .andExpect(content().string(not(containsString("draft player summary"))));
    }

    @Test
    void integrityChecksArePublicAndSanitized() throws Exception {
        PrematchFixture fixture = createPrematchFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/prematch-workbench/matches/" + fixture.matchId() + "/integrity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].status").exists()));
    }

    private ResultActions expectNoForbiddenFieldsOrTokens(ResultActions result) throws Exception {
        return result
                .andExpect(jsonPath("$..rawPayload").doesNotExist())
                .andExpect(jsonPath("$..payload").doesNotExist())
                .andExpect(jsonPath("$..ticketNo").doesNotExist())
                .andExpect(jsonPath("$..stake").doesNotExist())
                .andExpect(jsonPath("$..stakeSuggestion").doesNotExist())
                .andExpect(jsonPath("$..budgetAmount").doesNotExist())
                .andExpect(jsonPath("$..returnAmount").doesNotExist())
                .andExpect(jsonPath("$..profitLoss").doesNotExist())
                .andExpect(jsonPath("$..approvedBy").doesNotExist())
                .andExpect(jsonPath("$..reviewedBy").doesNotExist())
                .andExpect(jsonPath("$..reviewNote").doesNotExist())
                .andExpect(content().string(not(containsString("rawPayload"))))
                .andExpect(content().string(not(containsString("payload"))))
                .andExpect(content().string(not(containsString("ticketNo"))))
                .andExpect(content().string(not(containsString("stake"))))
                .andExpect(content().string(not(containsString("stakeSuggestion"))))
                .andExpect(content().string(not(containsString("budgetAmount"))))
                .andExpect(content().string(not(containsString("returnAmount"))))
                .andExpect(content().string(not(containsString("profitLoss"))))
                .andExpect(content().string(not(containsString("approvedBy"))))
                .andExpect(content().string(not(containsString("reviewedBy"))))
                .andExpect(content().string(not(containsString("reviewNote"))))
                .andExpect(content().string(not(containsString("SECRET"))));
    }

    private PrematchFixture createPrematchFixture() {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("prematch-home", "Public Home");
        long awayTeamId = insertTeam("prematch-away", "Public Away");
        long playerId = insertPlayer("prematch-player", homeTeamId, "Public Player");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertTeamFact(homeTeamId);
        insertTeamFact(awayTeamId);
        insertPlayerFact(playerId);
        insertLineup(matchId, homeTeamId, playerId);
        insertEvidence(matchId);
        insertConflict(matchId);
        long marketId = insertOddsMarket(importItemId, matchId);
        insertOddsSelection(marketId);
        long factorId = insertSentimentFactor(importItemId, matchId);
        insertSentimentRisk(importItemId, matchId, factorId);
        long reportId = insertAnalysisReport(importItemId, matchId);
        long planId = insertBetPlan(importItemId, matchId, reportId);
        insertBetPlanItem(planId, matchId);
        insertBet(importItemId, matchId);
        return new PrematchFixture(matchId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "C:/SECRET/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "PREMATCH", "APPROVED", "prematch.json", "2".repeat(64), "prematch", true, "ok", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
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
        return insertMatch("prematch-home-away-20260623", "Public Home vs Public Away", homeTeamId, awayTeamId);
    }

    private long insertMatch(String matchKey, String matchName, long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                matchKey, matchName, "2026-06-23", "031", "World Cup", "Group", "Test Stadium",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "weather clear reviewedBy=SECRET", "{\"payload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, matchKey);
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

    private void insertLineup(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, "STARTER", "FW", true);
    }

    private void insertEvidence(long matchId) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", "lineup", "https://example.test/fifa", Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 18, 0)),
                "official source ticketNo=SECRET", "9.5", "{\"official\":\"SECRET\"}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "prematch-home-away-20260623", "payload.ticketNo", "ticketNo=SECRET", "stake=88", "PENDING", "{\"field\":\"SECRET\"}");
    }

    private long insertOddsMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, captured_at, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "Pinnacle", "HAD", "Win Draw Win", "OPEN", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)),
                "source reviewedBy=SECRET", "{\"market\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM odds_market_snapshots", Long.class);
    }

    private void insertOddsSelection(long marketId) {
        jdbcTemplate.update("INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                marketId, "HOME", "Home win", "1.8000", "0.555556", "ACTIVE", "{\"selection\":\"SECRET\"}");
    }

    private long insertSentimentFactor(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, entity_type, entity_key, evidence_level, source_name, source_ref, observed_at, expires_at, confidence_score, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "Light rain", "summary reviewedBy=SECRET", "MIXED", "MATCH", "prematch-home-away-20260623", "DATA_VENDOR",
                "Weather Provider", "source", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 10, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 6, 24, 10, 0)),
                "7.5", "8.0", "{\"category\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
    }

    private void insertSentimentRisk(long importItemId, long matchId, long factorId) {
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "PUBLIC_OVERHEAT", "HIGH", "78.0000", "Overheat", "rationale approvedBy=SECRET", "LOWER_CONFIDENCE", "Media Digest", "source", "{\"type\":\"SECRET\"}");
    }

    private long insertAnalysisReport(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "prematch-report-1", "VALUE", "HIGH", "risk ticketNo=SECRET", "HAD", "dimensions", "narrative SECRET", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM analysis_reports", Long.class);
    }

    private long insertBetPlan(long importItemId, long matchId, long reportId) {
        jdbcTemplate.update("INSERT INTO bet_plans(import_item_id, analysis_report_id, match_id, plan_key, plan_title, conclusion_type, confidence, budget_amount, risk_summary, betting_method, strategy_type, status, generated_by, generated_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, reportId, matchId, "plan-1", "Plan SECRET", "VALUE", "HIGH", "100.00", "risk budgetAmount=100", "HAD", "SINGLE", "READY", "admin",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 13, 0)), "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM bet_plans", Long.class);
    }

    private void insertBetPlanItem(long planId, long matchId) {
        jdbcTemplate.update("INSERT INTO bet_plan_items(bet_plan_id, match_id, market_type, selection_text, stake_suggestion, odds, line_value, logic_type, risk_level, play_type, pass_type, item_order, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                planId, matchId, "HAD", "Home SECRET", "30.00", "1.80", null, "VALUE", "HIGH", "SINGLE", "1x1", 1, "{\"rawPayload\":\"SECRET\"}");
    }

    private void insertBet(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO bets(import_item_id, match_id, bet_id, ticket_no, bet_date, matchday, match_name, market_type, selection_text, stake, odds, closing_odds, clv, return_amount, hit_status, profit_loss, review_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "bet-1", "SECRET-TICKET", "2026-06-22", "2026-06-23", "Public Home vs Public Away", "HAD", "Home", "30.00", "1.80", "1.70", "0.058823", "54.00", "PENDING", "24.00", "UNREVIEWED", "{\"rawPayload\":\"SECRET\"}");
    }

    private record PrematchFixture(long matchId) {
    }
}
