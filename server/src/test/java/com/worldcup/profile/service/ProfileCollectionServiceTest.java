package com.worldcup.profile.service;

import com.worldcup.profile.api.dto.ProfileDtos.CollectionItemReviewResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProfileCollectionServiceTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ProfileCollectionService service;

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
    void approveTeamCollectionItemCreatesOfficialFactAndIsIdempotent() {
        insertTeam("argentina", "阿根廷");
        long itemId = insertCollectionItem("TEAM", "argentina", "STYLE", "高位逼抢", "梅西带动前场压迫", "Opta");

        CollectionItemReviewResponse first = service.approveItem(itemId, "admin");
        CollectionItemReviewResponse second = service.approveItem(itemId, "admin");

        assertThat(first.status()).isEqualTo("APPROVED");
        assertThat(first.targetType()).isEqualTo("TEAM_PROFILE_FACT");
        assertThat(second.targetId()).isEqualTo(first.targetId());
        assertThat(count("team_profile_facts")).isEqualTo(1);
        assertThat(count("player_profile_facts")).isZero();
        assertThat(statusOf(itemId)).isEqualTo("APPROVED");
    }

    @Test
    void approvePlayerCollectionItemCreatesOfficialFact() {
        long teamId = insertTeam("france", "法国");
        insertPlayer("mbappe", teamId, "姆巴佩");
        long itemId = insertCollectionItem("PLAYER", "mbappe", "INJURY", "伤病更新", "恢复合练，可以首发", "队报");

        CollectionItemReviewResponse response = service.approveItem(itemId, "admin");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.targetType()).isEqualTo("PLAYER_PROFILE_FACT");
        assertThat(count("player_profile_facts")).isEqualTo(1);
        assertThat(count("team_profile_facts")).isZero();
    }

    @Test
    void approveUnknownEntityReturnsBadRequestAndDoesNotCreateFact() {
        long itemId = insertCollectionItem("TEAM", "ghost-team", "SENTIMENT", "舆情", "来源无法匹配球队", "News");

        assertThatThrownBy(() -> service.approveItem(itemId, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不存在");
        assertThat(count("team_profile_facts")).isZero();
        assertThat(statusOf(itemId)).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void rejectPendingCollectionItemDoesNotCreateOfficialFact() {
        insertTeam("brazil", "巴西");
        long itemId = insertCollectionItem("TEAM", "brazil", "LOCKER_ROOM", "更衣室传闻", "来源可信度不足", "Rumor");

        CollectionItemReviewResponse response = service.rejectItem(itemId, "来源不足", "admin");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.targetType()).isNull();
        assertThat(count("team_profile_facts")).isZero();
        assertThat(statusOf(itemId)).isEqualTo("REJECTED");
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, style_tags, attack_profile, defense_profile, public_sentiment) VALUES (?,?,?,?,?,?,?)",
                key, name, key.substring(0, Math.min(3, key.length())).toUpperCase(), "控球,压迫", "边路推进", "高位反抢", "整体正面");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key = ?", Long.class, key);
    }

    private void insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, status, injury_status, card_status, locker_room_status) VALUES (?,?,?,?,?,?,?,?,?)",
                key, teamId, name, 10, "FW", "FIT", "无", "无", "稳定");
    }

    private long insertCollectionItem(String entityType, String entityKey, String factType, String title, String summary, String sourceName) {
        jdbcTemplate.update("INSERT INTO collection_jobs(source_type, source_name, keyword, status, triggered_by, message, total_items, pending_items) VALUES (?,?,?,?,?,?,?,?)",
                "MANUAL", sourceName, entityKey, "COMPLETED", "test", "ok", 1, 1);
        Long jobId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM collection_jobs", Long.class);
        jdbcTemplate.update("INSERT INTO collection_items(job_id, entity_type, entity_key, fact_type, title, summary, source_name, source_url, source_ref, reliability_score, confidence_score, raw_payload, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                jobId, entityType, entityKey, factType, title, summary, sourceName, "https://example.test", "ref-1", "8.5", "7.5", "{\"summary\":\"" + summary + "\"}", "PENDING_REVIEW");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM collection_items", Long.class);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String statusOf(long itemId) {
        return jdbcTemplate.queryForObject("SELECT status FROM collection_items WHERE id = ?", String.class, itemId);
    }
}
