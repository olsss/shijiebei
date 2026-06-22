package com.worldcup.prematchworkbench.api;

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
class PrematchWorkbenchControllerTest {
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
        jdbcTemplate.update("DELETE FROM player_profile_facts");
        jdbcTemplate.update("DELETE FROM team_profile_facts");
        jdbcTemplate.update("DELETE FROM collection_items");
        jdbcTemplate.update("DELETE FROM collection_jobs");
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
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void prematchWorkbenchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/prematch-workbench/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchListReturnsIntegritySummary() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/prematch-workbench/matches").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].matchName").value("法国 vs 巴西"))
                .andExpect(jsonPath("$.data[0].homeTeamName").value("法国"))
                .andExpect(jsonPath("$.data[0].awayTeamName").value("巴西"))
                .andExpect(jsonPath("$.data[0].integrityScore").value(90))
                .andExpect(jsonPath("$.data[0].missingCount").value(0))
                .andExpect(jsonPath("$.data[0].staleCount").value(0))
                .andExpect(jsonPath("$.data[0].conflictCount").value(1))
                .andExpect(jsonPath("$.data[0].oddsMarketCount").value(1))
                .andExpect(jsonPath("$.data[0].analysisReportCount").value(1))
                .andExpect(jsonPath("$.data[0].betPlanCount").value(1));
    }

    @Test
    void matchDetailAggregatesAllWorkbenchSections() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/prematch-workbench/matches/" + fixture.matchId()).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.matchId").value(fixture.matchId()))
                .andExpect(jsonPath("$.data.teams[0].teamName").value("法国"))
                .andExpect(jsonPath("$.data.teams[0].facts[0].title").value("法国高位压迫"))
                .andExpect(jsonPath("$.data.lineups[0].playerName").value("姆巴佩"))
                .andExpect(jsonPath("$.data.players[0].injuryStatus").value("无"))
                .andExpect(jsonPath("$.data.players[0].facts[0].title").value("姆巴佩状态稳定"))
                .andExpect(jsonPath("$.data.oddsMarkets[0].marketCode").value("HAD"))
                .andExpect(jsonPath("$.data.oddsMarkets[0].selections[0].selectionName").value("主胜"))
                .andExpect(jsonPath("$.data.sentimentFactors[0].factorCategory").value("WEATHER"))
                .andExpect(jsonPath("$.data.sentimentFactors[0].risks[0].riskType").value("RAIN"))
                .andExpect(jsonPath("$.data.evidence[0].sourceName").value("FIFA"))
                .andExpect(jsonPath("$.data.conflicts[0].resolutionStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.analysisReports[0].analysisId").value("analysis-france-brazil"))
                .andExpect(jsonPath("$.data.betPlans[0].planKey").value("plan-france-brazil"))
                .andExpect(jsonPath("$.data.betPlans[0].items[0].selectionText").value("主胜"))
                .andExpect(jsonPath("$.data.bets[0].ticketNo").value("T-001"))
                .andExpect(jsonPath("$.data.integrityChecks[?(@.code == 'UNRESOLVED_CONFLICT')].status").value("CONFLICT"));
    }

    @Test
    void integrityEndpointReturnsChecksAndUnknownMatchReturnsNotFound() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/prematch-workbench/matches/" + fixture.matchId() + "/integrity").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[?(@.code == 'TEAM_PROFILE')].status").value("PASS"))
                .andExpect(jsonPath("$.data[?(@.code == 'LIVE_ODDS_FRESHNESS')].status").value("PASS"))
                .andExpect(jsonPath("$.data[?(@.code == 'UNRESOLVED_CONFLICT')].status").value("CONFLICT"));

        mockMvc.perform(get("/api/prematch-workbench/matches/999999").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isNotFound());
    }

    private Fixture createFixture() {
        long importItemId = insertImportItem();
        long franceId = insertTeam("france", "法国");
        long brazilId = insertTeam("brazil", "巴西");
        long mbappeId = insertPlayer("mbappe", franceId, "姆巴佩", 10, "LW");
        long viniciusId = insertPlayer("vinicius", brazilId, "维尼修斯", 7, "LW");
        long matchId = insertMatch(franceId, brazilId);
        insertTeamFact(franceId, "法国高位压迫");
        insertTeamFact(brazilId, "巴西边路爆点");
        insertPlayerFact(mbappeId, "姆巴佩状态稳定");
        insertPlayerFact(viniciusId, "维尼修斯突破状态好");
        insertLineup(matchId, franceId, mbappeId, "STARTER", "LW", true);
        insertLineup(matchId, brazilId, viniciusId, "STARTER", "LW", true);
        long marketId = insertOddsMarket(importItemId, matchId);
        insertSelection(marketId, "HOME", "主胜", "1.85");
        insertSentiment(importItemId, matchId);
        insertEvidence(matchId, "FIFA", "official-lineup");
        insertEvidence(matchId, "Opta", "stats-pack");
        insertConflict(matchId);
        long reportId = insertAnalysisReport(importItemId, matchId);
        long planId = insertBetPlan(importItemId, matchId, reportId);
        insertBetPlanItem(planId, matchId);
        insertBet(importItemId, matchId);
        return new Fixture(matchId);
    }

    private long insertImportItem() {
        jdbcTemplate.update("INSERT INTO import_jobs(archive_path, status, total_items, valid_items, invalid_items, message) VALUES (?,?,?,?,?,?)",
                "test/archive", "SCANNED", 1, 1, 0, "ok");
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO import_items(job_id, item_type, status, relative_path, sha256, summary_title, valid_json, validation_message, raw_json) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, "ANALYSIS", "APPROVED", "analysis.json", "0".repeat(64), "法国 vs 巴西", true, "ok", "{}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM import_items", Long.class);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "高压", "边路", "回追", "稳定", "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name, int shirtNumber, String position) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                key, teamId, name, shirtNumber, position, "FIT", "无", "无", "稳定", "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "france-brazil-20260626", "法国 vs 巴西", "2026-06-26", "周五001", "世界杯", "小组赛", "纽约", Timestamp.valueOf(LocalDateTime.now().plusDays(3)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING", "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "france-brazil-20260626");
    }

    private void insertTeamFact(long teamId, String title) {
        jdbcTemplate.update("INSERT INTO team_profile_facts(team_id, fact_type, title, summary, source_name, captured_at, raw_payload) VALUES (?,?,?,?,?,?,?)",
                teamId, "STYLE", title, title + " 摘要", "Scout", Timestamp.valueOf(LocalDateTime.now().minusHours(1)), "{}");
    }

    private void insertPlayerFact(long playerId, String title) {
        jdbcTemplate.update("INSERT INTO player_profile_facts(player_id, fact_type, title, summary, source_name, captured_at, raw_payload) VALUES (?,?,?,?,?,?,?)",
                playerId, "FORM", title, title + " 摘要", "Scout", Timestamp.valueOf(LocalDateTime.now().minusHours(1)), "{}");
    }

    private void insertLineup(long matchId, long teamId, long playerId, String role, String position, boolean starter) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, role, position, starter);
    }

    private long insertOddsMarket(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO odds_market_snapshots(import_item_id, match_id, bookmaker, market_code, market_name, snapshot_type, line_value, captured_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "Pinnacle", "HAD", "胜平负", "LIVE", "0", Timestamp.valueOf(LocalDateTime.now().minusMinutes(30)), "{}");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM odds_market_snapshots", Long.class);
    }

    private void insertSelection(long marketId, String code, String name, String odds) {
        jdbcTemplate.update("INSERT INTO odds_selection_snapshots(market_snapshot_id, selection_code, selection_name, odds_value, selection_status, raw_payload) VALUES (?,?,?,?,?,?)",
                marketId, code, name, odds, "OPEN", "{}");
    }

    private void insertSentiment(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO match_context_factors(import_item_id, match_id, factor_category, factor_type, title, summary, impact_direction, evidence_level, source_name, observed_at, expires_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "WEATHER", "RAIN", "小雨", "预计小雨", "MIXED", "DATA_VENDOR", "Weather", Timestamp.valueOf(LocalDateTime.now().minusHours(1)), Timestamp.valueOf(LocalDateTime.now().plusHours(2)), "{}");
        Long factorId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM match_context_factors", Long.class);
        jdbcTemplate.update("INSERT INTO sentiment_risk_assessments(import_item_id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, factorId, "RAIN", "LOW", "20", "小雨风险", "草皮偏滑", "MONITOR", "Weather", "{}");
    }

    private void insertEvidence(long matchId, String sourceName, String sourceRef) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", sourceName, sourceRef, "https://example.test/" + sourceRef, Timestamp.valueOf(LocalDateTime.now().minusHours(1)), sourceName + " 证据", "9.0", "{}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "france-brazil-20260626", "lineup", "旧首发", "新首发", "PENDING", "{}");
    }

    private long insertAnalysisReport(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO analysis_reports(import_item_id, match_id, analysis_id, conclusion_type, confidence, risk_summary, recommended_markets, dimensions, narrative_md, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "analysis-france-brazil", "盘口判断", "中高", "[]", "[]", "{}", "正文", "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM analysis_reports WHERE analysis_id=?", Long.class, "analysis-france-brazil");
    }

    private long insertBetPlan(long importItemId, long matchId, long reportId) {
        jdbcTemplate.update("INSERT INTO bet_plans(import_item_id, analysis_report_id, match_id, plan_key, plan_title, conclusion_type, confidence, budget_amount, risk_summary, betting_method, strategy_type, status, generated_by, generated_at, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, reportId, matchId, "plan-france-brazil", "法国方向组合", "盘口判断", "MEDIUM_HIGH", "100", "控制投入", "AI_VALUE_SPLIT", "主线加保险", "IMPORTED", "codex", Timestamp.valueOf(LocalDateTime.now().minusHours(1)), "{}");
        return jdbcTemplate.queryForObject("SELECT id FROM bet_plans WHERE plan_key=?", Long.class, "plan-france-brazil");
    }

    private void insertBetPlanItem(long planId, long matchId) {
        jdbcTemplate.update("INSERT INTO bet_plan_items(bet_plan_id, match_id, market_type, selection_text, stake_suggestion, odds, logic_type, risk_level, play_type, pass_type, item_order, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                planId, matchId, "HAD", "主胜", "60", "1.85", "MAIN", "MEDIUM", "单关", "单关", 0, "{}");
    }

    private void insertBet(long importItemId, long matchId) {
        jdbcTemplate.update("INSERT INTO bets(import_item_id, match_id, bet_id, ticket_no, bet_date, matchday, match_name, market_type, selection_text, stake, odds, closing_odds, clv, return_amount, hit_status, profit_loss, review_status, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                importItemId, matchId, "bet-1", "T-001", "2026-06-22", "2026-06-26", "法国 vs 巴西", "HAD", "主胜", "60", "1.95", "1.80", "0.083333", "0", "PENDING", "0", "UNREVIEWED", "{}");
    }

    private record Fixture(long matchId) {
    }
}
