package com.worldcup.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicOverviewControllerTest {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 6, 23, 12, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW.atZone(BEIJING_ZONE).toInstant(), BEIJING_ZONE);
        }
    }

    @BeforeEach
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
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM import_items");
        jdbcTemplate.update("DELETE FROM import_jobs");
    }

    @Test
    void overviewIsPublicAndDoesNotExposeSensitiveFieldsOrTokens() throws Exception {
        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt").exists())
                .andExpect(jsonPath("$.data.upcomingMatches").isArray())
                .andExpect(jsonPath("$.data.riskCounters").exists())
                .andExpect(jsonPath("$.data.integrityCounters").exists())
                .andExpect(jsonPath("$.data.oddsFreshness").exists())
                .andExpect(jsonPath("$.data.decisionSummary").exists())
                .andExpect(jsonPath("$.data.adminTodoCounters").doesNotExist()));
    }

    @Test
    void overviewPrioritizesTodayMatchesAndSkipsPastRows() throws Exception {
        LocalDate today = FIXED_NOW.toLocalDate();
        insertOverviewMatch("past-match", "Historical Match", today.minusDays(1),
                LocalDateTime.of(today.minusDays(1), LocalTime.of(12, 0)));
        insertOverviewMatch("today-morning", "Today Morning", today,
                LocalDateTime.of(today, LocalTime.of(14, 0)));
        insertOverviewMatch("today-evening", "Today Evening", today,
                LocalDateTime.of(today, LocalTime.of(20, 0)));
        insertOverviewMatch("future-match", "Future Match", today.plusDays(1),
                LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)));

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.upcomingMatches", hasSize(2)))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].matchName").value("Today Morning"))
                        .andExpect(jsonPath("$.data.upcomingMatches[1].matchName").value("Today Evening")))
                .andExpect(content().string(not(containsString("Historical Match"))))
                .andExpect(content().string(not(containsString("Future Match"))));
    }

    @Test
    void overviewFallsBackToNearestFutureMatchesWhenTodayIsEmpty() throws Exception {
        LocalDate today = FIXED_NOW.toLocalDate();
        insertOverviewMatch("past-match", "Historical Match", today.minusDays(1),
                LocalDateTime.of(today.minusDays(1), LocalTime.of(12, 0)));
        insertOverviewMatch("tomorrow-noon", "Tomorrow Noon", today.plusDays(1),
                LocalDateTime.of(today.plusDays(1), LocalTime.of(12, 0)));
        insertOverviewMatch("tomorrow-night", "Tomorrow Night", today.plusDays(1),
                LocalDateTime.of(today.plusDays(1), LocalTime.of(22, 0)));
        insertOverviewMatch("future-later", "Future Later", today.plusDays(3),
                LocalDateTime.of(today.plusDays(3), LocalTime.of(18, 0)));

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.upcomingMatches", hasSize(3)))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].matchName").value("Tomorrow Noon"))
                        .andExpect(jsonPath("$.data.upcomingMatches[1].matchName").value("Tomorrow Night"))
                        .andExpect(jsonPath("$.data.upcomingMatches[2].matchName").value("Future Later")))
                .andExpect(content().string(not(containsString("Historical Match"))));
    }

    @Test
    void overviewFallsBackToRecentScoreRowsWhenNoTodayOrFutureMatchesExist() throws Exception {
        LocalDate today = FIXED_NOW.toLocalDate();
        long homeTeamId = insertTeam("recent-home", "Recent Home");
        long awayTeamId = insertTeam("recent-away", "Recent Away");
        insertOverviewMatch("recent-empty", "Recent Empty", today.minusDays(1),
                LocalDateTime.of(today.minusDays(1), LocalTime.of(21, 0)));
        long recentScoredId = insertOverviewMatchWithTeams(
                "recent-scored",
                "Recent Scored",
                today.minusDays(2),
                LocalDateTime.of(today.minusDays(2), LocalTime.of(20, 0)),
                homeTeamId,
                awayTeamId
        );
        long olderScoredId = insertOverviewMatchWithTeams(
                "older-scored",
                "Older Scored",
                today.minusDays(3),
                LocalDateTime.of(today.minusDays(3), LocalTime.of(18, 0)),
                homeTeamId,
                awayTeamId
        );
        insertEvent(recentScoredId, homeTeamId, "GOAL");
        insertEvent(olderScoredId, awayTeamId, "GOAL");

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.upcomingMatches", hasSize(3)))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].matchName").value("Recent Scored"))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].scoreboard.scoreDisplay").value("1 - 0"))
                        .andExpect(jsonPath("$.data.upcomingMatches[1].matchName").value("Older Scored"))
                        .andExpect(jsonPath("$.data.upcomingMatches[2].matchName").value("Recent Empty")));
    }

    @Test
    void overviewDoesNotShowAlreadyFinishedTodayMatches() throws Exception {
        LocalDate today = FIXED_NOW.toLocalDate();
        insertOverviewMatch("today-finished", "Today Finished", today, FIXED_NOW.minusHours(2));
        insertOverviewMatch("next-available", "Next Available", today, FIXED_NOW.plusHours(1));

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.upcomingMatches", hasSize(1)))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].matchName").value("Next Available")))
                .andExpect(content().string(not(containsString("Today Finished"))));
    }

    @Test
    void overviewUsesGoalEventsAsScoreFallbackWhenTeamStatsAreMissing() throws Exception {
        LocalDate today = FIXED_NOW.toLocalDate();
        long homeTeamId = insertTeam("overview-home", "Overview Home");
        long awayTeamId = insertTeam("overview-away", "Overview Away");
        long matchId = insertOverviewMatchWithTeams(
                "overview-event-score",
                "Overview Home vs Overview Away",
                today,
                FIXED_NOW.plusHours(2),
                homeTeamId,
                awayTeamId
        );
        insertEvent(matchId, homeTeamId, "GOAL");
        insertEvent(matchId, awayTeamId, "PENALTY_SCORED");
        insertEvent(matchId, awayTeamId, "GOAL_FREE_KICK");
        insertEvent(matchId, homeTeamId, "YELLOW_CARD");

        expectNoForbiddenFieldsOrTokens(mockMvc.perform(get("/api/public/overview"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.upcomingMatches[0].scoreboard.scoreDisplay").value("1 - 2"))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].scoreboard.winnerSide").value("AWAY"))
                        .andExpect(jsonPath("$.data.upcomingMatches[0].scoreboard.scoreSource").value("MATCH_EVENTS")));
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

    private void insertOverviewMatch(String key, String name, LocalDate matchday, LocalDateTime kickoffTime) {
        jdbcTemplate.update("""
                        INSERT INTO matches(match_key, match_name, matchday, kickoff_time, status, result_status)
                        VALUES (?,?,?,?,?,?)
                        """,
                key, name, java.sql.Date.valueOf(matchday), Timestamp.valueOf(kickoffTime),
                "SCHEDULED", "PENDING");
    }

    private long insertOverviewMatchWithTeams(String key, String name, LocalDate matchday, LocalDateTime kickoffTime, long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("""
                        INSERT INTO matches(match_key, match_name, matchday, kickoff_time, home_team_id, away_team_id, status, result_status)
                        VALUES (?,?,?,?,?,?,?,?)
                        """,
                key, name, java.sql.Date.valueOf(matchday), Timestamp.valueOf(kickoffTime), homeTeamId, awayTeamId,
                "LIVE", "IN_PLAY");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, key);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, raw_payload) VALUES (?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private void insertEvent(long matchId, long teamId, String eventType) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, payload) VALUES (?,?,?,?,?)",
                matchId, 12, eventType, teamId, "{\"payload\":\"SECRET\"}");
    }
}
