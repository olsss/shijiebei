package com.worldcup.publicapi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.worldcup.analysisreviewcenter.service.AnalysisReviewCenterQueryService;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicDecisionsControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    AnalysisReviewCenterQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM review_lessons");
        jdbcTemplate.update("DELETE FROM post_match_reviews");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM bet_plan_items");
        jdbcTemplate.update("DELETE FROM bet_plans");
        jdbcTemplate.update("DELETE FROM analysis_reports");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void reportsArePublicSummariesOnlyAndUseStoredReportData() throws Exception {
        DecisionFixture fixture = createDecisionFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/decisions/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.reportId()))
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("Public Home vs Public Away")));

        verify(richQueryService, never()).reports();
    }

    @Test
    void reviewsArePublicSummariesOnlyAndUseStoredReviewData() throws Exception {
        DecisionFixture fixture = createDecisionFixture();

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/decisions/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.reviewId()))
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("Public Home vs Public Away"))
                .andExpect(jsonPath("$.data[0].lessons.length()").value(1))
                .andExpect(jsonPath("$.data[0].lessons[0].lessonType").value("RISK"))
                .andExpect(jsonPath("$.data[0].lessons[0].severity").value("HIGH")));

        verify(richQueryService, never()).reviews();
    }

    @Test
    void reportsUseDefaultPublicLimit() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("report-limit-home", "Report Limit Home");
        long awayTeamId = insertTeam("report-limit-away", "Report Limit Away");
        long matchId = insertMatch("report-limit-match", "Report Limit Home vs Report Limit Away", homeTeamId, awayTeamId);
        for (int i = 0; i < 55; i++) {
            insertAnalysisReport(importItemId, matchId, "limit-report-" + i);
        }

        mockMvc.perform(get("/api/public/decisions/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(50)));
    }

    @Test
    void reviewsUseDefaultPublicLimit() throws Exception {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("review-limit-home", "Review Limit Home");
        long awayTeamId = insertTeam("review-limit-away", "Review Limit Away");
        long matchId = insertMatch("review-limit-match", "Review Limit Home vs Review Limit Away", homeTeamId, awayTeamId);
        long reportId = insertAnalysisReport(importItemId, matchId, "review-limit-report");
        for (int i = 0; i < 55; i++) {
            insertReview(importItemId, matchId, reportId, "limit-review-" + i);
        }

        mockMvc.perform(get("/api/public/decisions/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(50)));
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

    private DecisionFixture createDecisionFixture() {
        long importItemId = insertImportItem();
        long homeTeamId = insertTeam("decision-home", "Public Home");
        long awayTeamId = insertTeam("decision-away", "Public Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        long reportId = insertAnalysisReport(importItemId, matchId);
        long reviewId = insertReview(importItemId, matchId, reportId);
        insertReviewLesson(reviewId);
        return new DecisionFixture(matchId, reportId, reviewId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "C:/SECRET/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "ANALYSIS", "APPROVED", "decision.json", "1".repeat(64), "decision", true, "ok", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code) VALUES (?,?,?)", key, name, key.substring(0, 3).toUpperCase());
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        return insertMatch("decision-home-away-20260623", "Public Home vs Public Away", homeTeamId, awayTeamId);
    }

    private long insertMatch(String matchKey, String matchName, long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                matchKey, matchName, "2026-06-23", "031", "World Cup", "Group", "Test Stadium",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "approvedBy=SECRET", "{\"payload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, matchKey);
    }

    private long insertAnalysisReport(long importItemId, long matchId) {
        return insertAnalysisReport(importItemId, matchId, "decision-report-1");
    }

    private long insertAnalysisReport(long importItemId, long matchId, String analysisId) {
        jdbcTemplate.update("INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, analysisId, "VALUE", "HIGH",
                "risk summary ticketNo=SECRET stake=88 reviewedBy=admin rawPayload=SECRET",
                "HAD", "dimensions", "long narrative SECRET profitLoss=-20", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM analysis_reports", Long.class);
    }

    private long insertReview(long importItemId, long matchId, long reportId) {
        return insertReview(importItemId, matchId, reportId, "decision-review-1");
    }

    private long insertReview(long importItemId, long matchId, long reportId, String reviewKey) {
        jdbcTemplate.update("INSERT INTO post_match_reviews(import_item_id, match_id, analysis_report_id, review_key, review_title, math_review, football_review, handicap_review, tournament_temperament_review, odds_value_review, overall_summary, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, reportId, reviewKey, "Public review",
                "math ticketNo=SECRET", "football stakeSuggestion=88", "handicap budgetAmount=100",
                "temperament returnAmount=200", "odds profitLoss=-20", "overall reviewedBy=admin SECRET",
                "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM post_match_reviews", Long.class);
    }

    private void insertReviewLesson(long reviewId) {
        jdbcTemplate.update("INSERT INTO review_lessons(review_id, lesson_type, lesson_text, severity, raw_payload) VALUES (?,?,?,?,?)",
                reviewId, "RISK", "lesson approvedBy=SECRET", "HIGH", "{\"rawPayload\":\"SECRET\"}");
    }

    private record DecisionFixture(long matchId, long reportId, long reviewId) {
    }
}
