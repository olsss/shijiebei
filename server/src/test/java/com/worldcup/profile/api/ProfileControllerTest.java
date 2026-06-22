package com.worldcup.profile.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
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
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM matches");
    }

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/profiles/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teamListAndDetailReturnProfileFactsAndPlayers() throws Exception {
        long teamId = insertTeam("spain", "西班牙");
        insertPlayer("morata", teamId, "莫拉塔");
        insertTeamFact(teamId, "SCORING_PATTERN", "进球时间点", "60分钟后进球占比高");

        mockMvc.perform(get("/api/profiles/teams").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayName").value("西班牙"))
                .andExpect(jsonPath("$.data[0].playerCount").value(1))
                .andExpect(jsonPath("$.data[0].factCount").value(1));

        mockMvc.perform(get("/api/profiles/teams/" + teamId).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.displayName").value("西班牙"))
                .andExpect(jsonPath("$.data.facts[0].factType").value("SCORING_PATTERN"))
                .andExpect(jsonPath("$.data.players[0].displayName").value("莫拉塔"));
    }

    @Test
    void playerListAndDetailReturnStatusAndFacts() throws Exception {
        long teamId = insertTeam("england", "英格兰");
        long playerId = insertPlayer("bellingham", teamId, "贝林厄姆");
        insertPlayerFact(playerId, "FORM", "近期状态", "连续两场参与进球");

        mockMvc.perform(get("/api/profiles/players").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayName").value("贝林厄姆"))
                .andExpect(jsonPath("$.data[0].teamName").value("英格兰"))
                .andExpect(jsonPath("$.data[0].factCount").value(1));

        mockMvc.perform(get("/api/profiles/players/" + playerId).with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.displayName").value("贝林厄姆"))
                .andExpect(jsonPath("$.data.facts[0].factType").value("FORM"));
    }

    @Test
    void collectionItemCanBeListedAndApprovedThroughApi() throws Exception {
        insertTeam("netherlands", "荷兰");
        long itemId = insertCollectionItem("TEAM", "netherlands", "SENTIMENT", "舆情", "主流媒体评价积极");

        mockMvc.perform(get("/api/profiles/collections/items?status=PENDING_REVIEW").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].factType").value("SENTIMENT"));

        mockMvc.perform(post("/api/profiles/collections/items/" + itemId + "/approve").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.targetType").value("TEAM_PROFILE_FACT"));
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment) VALUES (?,?,?,?,?,?,?)",
                key, name, key.substring(0, Math.min(3, key.length())).toUpperCase(), "传控", "渗透", "压迫", "稳定");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key = ?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status) VALUES (?,?,?,?,?,?,?,?,?)",
                key, teamId, name, 9, "MF", "FIT", "无", "无", "稳定");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key = ?", Long.class, key);
    }

    private void insertTeamFact(long teamId, String factType, String title, String summary) {
        jdbcTemplate.update("INSERT INTO team_profile_facts(team_id, fact_type, title, summary, source_name, reliability_score, approved_by) VALUES (?,?,?,?,?,?,?)",
                teamId, factType, title, summary, "test", "8.0", "admin");
    }

    private void insertPlayerFact(long playerId, String factType, String title, String summary) {
        jdbcTemplate.update("INSERT INTO player_profile_facts(player_id, fact_type, title, summary, source_name, reliability_score, approved_by) VALUES (?,?,?,?,?,?,?)",
                playerId, factType, title, summary, "test", "8.0", "admin");
    }

    private long insertCollectionItem(String entityType, String entityKey, String factType, String title, String summary) {
        jdbcTemplate.update("INSERT INTO collection_jobs(source_type, source_name, keyword, status, triggered_by, message, total_items, pending_items) VALUES (?,?,?,?,?,?,?,?)",
                "MANUAL", "News", entityKey, "COMPLETED", "test", "ok", 1, 1);
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM collection_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO collection_items(job_id, entity_type, entity_key, fact_type, title, summary, source_name, source_url, source_ref, reliability_score, confidence_score, raw_payload, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                jobId, entityType, entityKey, factType, title, summary, "News", "https://example.test", "ref-1", "8.0", "7.0", "{\"summary\":\"" + summary + "\"}", "PENDING_REVIEW");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM collection_items", Long.class);
    }
}
