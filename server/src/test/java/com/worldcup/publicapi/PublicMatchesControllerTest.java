package com.worldcup.publicapi;

import com.worldcup.matchcenter.service.MatchCenterQueryService;
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
class PublicMatchesControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @SpyBean
    MatchCenterQueryService richQueryService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM data_conflicts");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_player_stats");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
    }

    @Test
    void publicMatchEndpointsUsePublicReadModelInsteadOfRichMatchService() throws Exception {
        MatchFixture fixture = createMatchFixture();

        mockMvc.perform(get("/api/public/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fixture.matchId()))
                .andExpect(jsonPath("$.data[0].homeTeam.fifaCode").value("MAT"))
                .andExpect(jsonPath("$.data[0].awayTeam.fifaCode").value("MAT"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data[0].scoreboard.winnerSide").value("HOME"))
                .andExpect(jsonPath("$.data[0].scoreboard.resultText").value("主队胜"));
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.id").value(fixture.matchId()))
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreSource").value("TEAM_STATS"))
                .andExpect(jsonPath("$.data.evidence[0].qualityLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.evidence[0].freshnessStatus").value("FRESH"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("阵容 / 首发"))
                .andExpect(jsonPath("$.data.evidence[0].suggestedAction").value("可作为核心证据，但仍需与比分/阵容交叉核对"))
                .andExpect(jsonPath("$.data.evidence[0].rawPayload").doesNotExist());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/team-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
        mockMvc.perform(get("/api/public/matches/" + fixture.matchId() + "/player-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());

        verify(richQueryService, never()).matches();
        verify(richQueryService, never()).match(anyLong());
        verify(richQueryService, never()).lineups(anyLong());
        verify(richQueryService, never()).events(anyLong());
        verify(richQueryService, never()).teamStats(anyLong());
        verify(richQueryService, never()).playerStats(anyLong());
    }

    @Test
    void publicMatchEvidenceClassifiesVenueAttendanceAndBroadcastSources() throws Exception {
        long homeTeamId = insertTeam("venue-home", "Venue Home");
        long awayTeamId = insertTeam("venue-away", "Venue Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "BROADCAST", "ESPN Scoreboard", "espn-broadcast-760414", "转播来源：ESPN Scoreboard 记录本场转播平台为 FOX。仅用于转播信息追溯。");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("BROADCAST"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("场馆 / 上座 / 转播"));
    }

    @Test
    void publicMatchEvidenceClassifiesQualificationAndKnockoutContext() throws Exception {
        long homeTeamId = insertTeam("knockout-home", "Knockout Home");
        long awayTeamId = insertTeam("knockout-away", "Knockout Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "KNOCKOUT_CONTEXT", "DB derived tournament format", "derived:knockout-context:test", "32强淘汰赛赛制事实：胜者晋级16强，负者结束本届淘汰赛路径；不代表投注方向。");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("KNOCKOUT_CONTEXT"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("出线 / 淘汰赛路径"));
    }

    @Test
    void publicMatchEvidenceClassifiesRecentFormSources() throws Exception {
        long homeTeamId = insertTeam("form-home", "Form Home");
        long awayTeamId = insertTeam("form-away", "Form Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "FORM_DERIVED", "DB derived tournament form", "derived:form-derived:test", "近期赛果来源：正式库已完赛比分统计生成本届赛果走势；不代表未来胜率。 ");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("FORM_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("球队状态 / 近期赛果"));
    }

    @Test
    void publicMatchEvidenceClassifiesKeyPlayerContributionSources() throws Exception {
        long homeTeamId = insertTeam("key-player-home", "Key Player Home");
        long awayTeamId = insertTeam("key-player-away", "Key Player Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "KEY_PLAYER_CONTRIBUTION_DERIVED", "DB derived key player contribution", "derived:key-player-contribution:test", "关键球员进球参与来源：正式库球员进球助攻统计生成贡献分布；不代表预计首发或未来进球概率。 ");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("KEY_PLAYER_CONTRIBUTION_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("关键球员 / 进球参与"));
    }


    @Test
    void publicMatchEvidenceClassifiesGoalkeepingSaveProfileSources() throws Exception {
        long homeTeamId = insertTeam("goalkeeping-home", "Goalkeeping Home");
        long awayTeamId = insertTeam("goalkeeping-away", "Goalkeeping Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "GOALKEEPING_SAVE_PROFILE_DERIVED", "DB derived goalkeeping profile", "derived:goalkeeping-save-profile-evidence:test", "门将扑救画像派生来源：正式库 GK saves 与 goalsConceded 基础统计生成；仅用于基础统计事实追溯。 ");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("GOALKEEPING_SAVE_PROFILE_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("门将 / 扑救画像"));
    }

    @Test
    void publicMatchEvidenceClassifiesFoulOffsidePenaltyProfileSources() throws Exception {
        long homeTeamId = insertTeam("infraction-home", "Infraction Home");
        long awayTeamId = insertTeam("infraction-away", "Infraction Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "FOUL_OFFSIDE_PENALTY_PROFILE_DERIVED", "DB derived foul offside profile", "derived:foul-offside-penalty-evidence:test", "犯规/越位基础画像派生来源：正式库 ESPN 技术统计 foulsCommitted、offsides、penaltyKickShots 字段生成；仅用于基础统计事实追溯。 ");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("FOUL_OFFSIDE_PENALTY_PROFILE_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("犯规 / 越位样本"));
    }


    @Test
    void publicMatchEvidenceClassifiesSquadProfileSources() throws Exception {
        long homeTeamId = insertTeam("squad-home", "Squad Home");
        long awayTeamId = insertTeam("squad-away", "Squad Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "SQUAD_LIST", "FIFA 官方 Squad List PDF", "derived:squad-list-evidence:wc2026-r32:test", "阵容结构画像派生来源：使用双方 26 人名单的出生日期、身高、位置、赛前国家队出场和俱乐部字段生成；仅用于基础事实追溯。");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("SQUAD_LIST"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("阵容 / 名单结构画像"));
    }


    @Test
    void publicMatchEvidenceClassifiesLineupStructureSources() throws Exception {
        long homeTeamId = insertTeam("lineup-structure-home", "Lineup Structure Home");
        long awayTeamId = insertTeam("lineup-structure-away", "Lineup Structure Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "LINEUP_STRUCTURE_DERIVED", "ESPN Summary API / rosters 派生", "derived:lineup-structure-evidence:wc2026-r32:test", "首发位置结构画像派生来源：使用正式库 match_lineups 中双方开球前已完赛确认首发、替补与位置编码生成；仅用于基础阵容样本追溯。");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("LINEUP_STRUCTURE_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("阵容 / 首发位置结构"));
    }


    @Test
    void publicMatchEvidenceClassifiesMarketPriceSnapshotSources() throws Exception {
        long homeTeamId = insertTeam("market-price-home", "Market Price Home");
        long awayTeamId = insertTeam("market-price-away", "Market Price Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertEvidence(matchId, "MARKET_PRICE_SNAPSHOT_DERIVED", "ESPN Summary API / DraftKings odds", "derived:market-price-snapshot:test", "市场价格快照来源：DraftKings 记录赛前赔率价格与隐含概率；仅用于价格快照追溯。 ");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence[0].sourceType").value("MARKET_PRICE_SNAPSHOT_DERIVED"))
                .andExpect(jsonPath("$.data.evidence[0].supportsConclusion").value("市场 / 赔率信号"));
    }

    @Test
    void publicMatchesFallbackToEvidenceScoreWhenTeamStatsMissing() throws Exception {
        long homeTeamId = insertTeam("fallback-home", "Fallback Home");
        long awayTeamId = insertTeam("fallback-away", "Fallback Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        jdbcTemplate.update("UPDATE matches SET status='FINISHED', result_status='FINAL' WHERE id=?", matchId);
        insertEvidence(matchId, "官方赛果：Fallback Home vs Fallback Away；比分=3-3");

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreDisplay").value("3 - 3"))
                .andExpect(jsonPath("$.data.summary.scoreboard.winnerSide").value("DRAW"))
                .andExpect(jsonPath("$.data.summary.scoreboard.resultText").value("平局"))
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreSource").value("EVIDENCE_TEXT"));
    }

    @Test
    void publicMatchesFallbackToGoalEventsWhenTeamStatsAndEvidenceScoreAreMissing() throws Exception {
        long homeTeamId = insertTeam("event-home", "Event Home");
        long awayTeamId = insertTeam("event-away", "Event Away");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        jdbcTemplate.update("UPDATE matches SET status='FINISHED', result_status='FINAL' WHERE id=?", matchId);
        insertEvent(matchId, homeTeamId, "GOAL");
        insertEvent(matchId, homeTeamId, "PENALTY_SCORED");
        insertEvent(matchId, awayTeamId, "GOAL_HEADER");
        insertEvent(matchId, awayTeamId, "YELLOW_CARD");
        insertEvidence(matchId, "官方事件已入库，未在摘要写比分");

        mockMvc.perform(get("/api/public/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data[0].scoreboard.winnerSide").value("HOME"))
                .andExpect(jsonPath("$.data[0].scoreboard.scoreSource").value("MATCH_EVENTS"));

        mockMvc.perform(get("/api/public/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreDisplay").value("2 - 1"))
                .andExpect(jsonPath("$.data.summary.scoreboard.resultText").value("主队胜"))
                .andExpect(jsonPath("$.data.summary.scoreboard.scoreSource").value("MATCH_EVENTS"));
    }

    private MatchFixture createMatchFixture() {
        long homeTeamId = insertTeam("match-home", "Match Home");
        long awayTeamId = insertTeam("match-away", "Match Away");
        long playerId = insertPlayer("match-player", homeTeamId, "Match Player");
        long matchId = insertMatch(homeTeamId, awayTeamId);
        insertLineup(matchId, homeTeamId, playerId);
        insertEvent(matchId, homeTeamId, playerId);
        insertTeamStats(matchId, homeTeamId, 2, 1);
        insertTeamStats(matchId, awayTeamId, 1, 2);
        insertPlayerStats(matchId, playerId);
        insertEvidence(matchId);
        insertConflict(matchId);
        return new MatchFixture(matchId);
    }

    private long insertTeam(String key, String name) {
        jdbcTemplate.update("INSERT INTO teams(team_key, display_name, fifa_code, raw_payload) VALUES (?,?,?,?)",
                key, name, key.substring(0, 3).toUpperCase(), "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM teams WHERE team_key=?", Long.class, key);
    }

    private long insertPlayer(String key, long teamId, String name) {
        jdbcTemplate.update("INSERT INTO players(player_key, team_id, display_name, shirt_number, position, raw_payload) VALUES (?,?,?,?,?,?)",
                key, teamId, name, 9, "FW", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM players WHERE player_key=?", Long.class, key);
    }

    private long insertMatch(long homeTeamId, long awayTeamId) {
        jdbcTemplate.update("INSERT INTO matches(match_key, match_name, matchday, jc_code, competition, stage, venue, kickoff_time, home_team_id, away_team_id, status, result_status, external_factors, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "match-home-away-20260623", "Match Home vs Match Away", "2026-06-23", "051", "World Cup", "Group", "Test Stadium",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 23, 20, 0)), homeTeamId, awayTeamId, "SCHEDULED", "PENDING",
                "weather approvedBy=SECRET", "{\"rawPayload\":\"SECRET\"}");
        return jdbcTemplate.queryForObject("SELECT id FROM matches WHERE match_key=?", Long.class, "match-home-away-20260623");
    }

    private void insertLineup(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_lineups(match_id, team_id, player_id, role, position, is_starter) VALUES (?,?,?,?,?,?)",
                matchId, teamId, playerId, "STARTER", "FW", true);
    }

    private void insertEvent(long matchId, long teamId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, player_id, payload) VALUES (?,?,?,?,?,?)",
                matchId, 12, "GOAL", teamId, playerId, "{\"payload\":\"SECRET\"}");
    }

    private void insertEvent(long matchId, long teamId, String eventType) {
        jdbcTemplate.update("INSERT INTO match_events(match_id, event_minute, event_type, team_id, payload) VALUES (?,?,?,?,?)",
                matchId, 12, eventType, teamId, "{\"payload\":\"SECRET\"}");
    }

    private void insertTeamStats(long matchId, long teamId, int goalsFor, int goalsAgainst) {
        jdbcTemplate.update("INSERT INTO match_team_stats(match_id, team_id, stats_type, goals_for, goals_against, first_goal_minute, scoring_minutes, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, teamId, "OFFICIAL", goalsFor, goalsAgainst, 12, "12,77", "{\"shots\":\"SECRET\"}");
    }

    private void insertPlayerStats(long matchId, long playerId) {
        jdbcTemplate.update("INSERT INTO match_player_stats(match_id, player_id, minutes_played, goals, assists, yellow_cards, red_cards, payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, playerId, 90, 1, 1, 0, 0, "{\"xg\":\"SECRET\"}");
    }

    private void insertEvidence(long matchId) {
        insertEvidence(matchId, "official source ticketNo=SECRET");
    }

    private void insertEvidence(long matchId, String summary) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, "OFFICIAL", "FIFA", "lineup", "https://example.test/fifa",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)),
                summary, "9.5", "{\"official\":\"SECRET\"}");
    }

    private void insertEvidence(long matchId, String sourceType, String sourceName, String sourceRef, String summary) {
        jdbcTemplate.update("INSERT INTO source_evidence(match_id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score, raw_payload) VALUES (?,?,?,?,?,?,?,?,?)",
                matchId, sourceType, sourceName, sourceRef, "https://example.test/source",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)),
                summary, "8.2", "{\"source\":\"SECRET\"}");
    }

    private void insertConflict(long matchId) {
        jdbcTemplate.update("INSERT INTO data_conflicts(match_id, conflict_type, entity_key, field_name, current_value, incoming_value, resolution_status, raw_payload) VALUES (?,?,?,?,?,?,?,?)",
                matchId, "LINEUP", "match-home-away-20260623", "payload.ticketNo", "ticketNo=SECRET", "stake=88", "PENDING", "{\"field\":\"SECRET\"}");
    }

    private record MatchFixture(long matchId) {
    }
}
