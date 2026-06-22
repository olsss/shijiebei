package com.worldcup.analysisreviewcenter.api;

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
class AnalysisReviewCenterControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM review_lessons");
        jdbcTemplate.update("DELETE FROM post_match_reviews");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM bet_plan_items");
        jdbcTemplate.update("DELETE FROM bet_plans");
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM import_item_mappings");
        jdbcTemplate.update("DELETE FROM data_dictionaries");
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
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void analysisReviewEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/analysis-review/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void overviewReturnsReportBetAndReviewStats() throws Exception {
        createFixture();

        mockMvc.perform(get("/api/analysis-review/overview").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportCount").value(1))
                .andExpect(jsonPath("$.data.betPlanCount").value(1))
                .andExpect(jsonPath("$.data.betCount").value(1))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.totalStake").value(60.0))
                .andExpect(jsonPath("$.data.totalReturn").value(108.0))
                .andExpect(jsonPath("$.data.netProfit").value(48.0))
                .andExpect(jsonPath("$.data.roi").value(0.8))
                .andExpect(jsonPath("$.data.averageClv").value(0.083333));
    }

    @Test
    void reportsAndPlansReturnDetails() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/analysis-review/reports").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.reportId()))
                .andExpect(jsonPath("$.data[0].analysisId").value("analysis-plan-1"))
                .andExpect(jsonPath("$.data[0].matchName").value("法国 vs 巴西"))
                .andExpect(jsonPath("$.data[0].betPlanCount").value(1))
                .andExpect(jsonPath("$.data[0].reviewCount").value(1));

        mockMvc.perform(get("/api/analysis-review/reports/" + fixture.reportId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.report.id").value(fixture.reportId()))
                .andExpect(jsonPath("$.data.report.riskSummary").value("[\"临场首发未核\"]"))
                .andExpect(jsonPath("$.data.betPlans[0].id").value(fixture.planId()))
                .andExpect(jsonPath("$.data.reviews[0].id").value(fixture.reviewId()));

        mockMvc.perform(get("/api/analysis-review/bet-plans").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.planId()))
                .andExpect(jsonPath("$.data[0].planKey").value("analysis-plan-1"))
                .andExpect(jsonPath("$.data[0].bettingMethod").value("AI_VALUE_SPLIT"))
                .andExpect(jsonPath("$.data[0].itemCount").value(2));

        mockMvc.perform(get("/api/analysis-review/bet-plans/" + fixture.planId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.id").value(fixture.planId()))
                .andExpect(jsonPath("$.data.items[0].selectionText").value("主胜"))
                .andExpect(jsonPath("$.data.items[0].playType").value("单关"));
    }

    @Test
    void betsAndReviewsReturnRows() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/analysis-review/bets").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.betId()))
                .andExpect(jsonPath("$.data[0].ticketNo").value("T-001"))
                .andExpect(jsonPath("$.data[0].marketType").value("HAD"))
                .andExpect(jsonPath("$.data[0].closingOdds").value(1.8))
                .andExpect(jsonPath("$.data[0].clv").value(0.083333))
                .andExpect(jsonPath("$.data[0].reviewStatus").value("READY"));

        mockMvc.perform(get("/api/analysis-review/reviews").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.reviewId()))
                .andExpect(jsonPath("$.data[0].reviewKey").value("review-france-brazil"))
                .andExpect(jsonPath("$.data[0].lessons[0].lessonText").value("继续记录入场与收盘差"));
    }

    private Fixture createFixture() {
        long importItemId = insertImportItem();
        long matchId = insertMatch();
        long reportId = insertAnalysisReport(importItemId, matchId);
        long planId = insertBetPlan(importItemId, matchId, reportId);
        insertBetPlanItem(planId, matchId, "HAD", "主胜", "60", "1.85", "单关", 0);
        insertBetPlanItem(planId, matchId, "TTG", "2球", "20", "3.20", "总进球", 1);
        long betId = insertBet(importItemId, matchId);
        long reviewId = insertReview(importItemId, matchId, reportId);
        insertLesson(reviewId);
        return new Fixture(reportId, planId, betId, reviewId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "test/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "ANALYSIS", "APPROVED", "analysis.json", "0".repeat(64), "法国 vs 巴西", true, "ok", "{}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertMatch() {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, status, result_status, raw_payload) VALUES (?,?,?,?,?,?,?)",
                "france-brazil-20260626", "法国 vs 巴西", "2026-06-26", "周五001", "IMPORTED", "UNKNOWN", "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "france-brazil-20260626");
    }

    private long insertAnalysisReport(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "analysis-plan-1", "盘口判断", "中高", "[\"临场首发未核\"]", "[{\"type\":\"HAD\"}]", "{\"form\":\"ok\"}", "正文", "{\"id\":\"analysis-plan-1\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM analysis_reports WHERE analysis_id=?", Long.class, "analysis-plan-1");
    }

    private long insertBetPlan(long importItemId, long matchId, long reportId) {
        jdbcTemplate.update("INSERT INTO bet_plans(import_item_id, analysis_report_id, match_id, plan_key, plan_title, conclusion_type, confidence, budget_amount, risk_summary, betting_method, strategy_type, status, generated_by, generated_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, reportId, matchId, "analysis-plan-1", "法国方向组合", "盘口判断", "MEDIUM_HIGH", "100", "热门方向需控制投入", "AI_VALUE_SPLIT", "主线加保险", "IMPORTED", "codex", Timestamp.valueOf(LocalDateTime.of(2026, 6, 22, 12, 0)), "{\"plan_key\":\"analysis-plan-1\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM bet_plans WHERE plan_key=?", Long.class, "analysis-plan-1");
    }

    private void insertBetPlanItem(long planId, long matchId, String market, String selection, String stake, String odds, String playType, int order) {
        jdbcTemplate.update("INSERT INTO bet_plan_items(bet_plan_id, match_id, market_type, selection_text, stake_suggestion, odds, line_value, logic_type, risk_level, play_type, pass_type, item_order, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                planId, matchId, market, selection, stake, odds, "0", "MAIN", "MEDIUM", playType, "单关", order, "{\"selection\":\"" + selection + "\"}");
    }

    private long insertBet(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO bets(import_item_id, match_id, bet_id, ticket_no, bet_date, matchday, match_name, market_type, selection_text, stake, odds, closing_odds, clv, return_amount, hit_status, profit_loss, settled_at, review_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "b-clv-1", "T-001", "2026-06-22", "2026-06-26", "法国 vs 巴西", "HAD", "主胜", "60", "1.95", "1.80", "0.083333", "108", "HIT", "48", Timestamp.valueOf(LocalDateTime.of(2026, 6, 26, 23, 0)), "READY", "{\"bet_id\":\"b-clv-1\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM bets WHERE bet_id=?", Long.class, "b-clv-1");
    }

    private long insertReview(long importItemId, long matchId, long reportId) {
        jdbcTemplate.update("INSERT INTO post_match_reviews(import_item_id, match_id, analysis_report_id, review_key, review_title, math_review, football_review, handicap_review, tournament_temperament_review, odds_value_review, overall_summary, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, reportId, "review-france-brazil", "法国 vs 巴西复盘", "命中主线", "法国边路压制有效", "盘口未被卡线", "大赛稳定性体现", "入场赔率优于收盘", "判断符合赛前逻辑", "{\"review_key\":\"review-france-brazil\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM post_match_reviews WHERE review_key=?", Long.class, "review-france-brazil");
    }

    private void insertLesson(long reviewId) {
        jdbcTemplate.update("INSERT INTO review_lessons(review_id, lesson_type, lesson_text, severity, raw_payload) VALUES (?,?,?,?,?)",
                reviewId, "CLV", "继续记录入场与收盘差", "MEDIUM", "{\"type\":\"CLV\"}");
    }

    private record Fixture(long reportId, long planId, long betId, long reviewId) {
    }
}
