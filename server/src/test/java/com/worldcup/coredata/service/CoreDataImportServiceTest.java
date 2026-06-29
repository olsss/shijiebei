package com.worldcup.coredata.service;

import com.worldcup.coredata.api.dto.CoreDataImportResponse;
import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import com.worldcup.importreview.domain.ImportJob;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.repo.ImportJobRepository;
import com.worldcup.importreview.service.JsonImportReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CoreDataImportServiceTest {
    @Autowired
    ImportJobRepository jobRepository;

    @Autowired
    ImportItemRepository itemRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    CoreDataImportService service;

    @Autowired
    JsonImportReviewService reviewService;

    @TempDir
    Path tempDir;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM match_market_signals");
        jdbcTemplate.update("DELETE FROM player_metric_snapshots");
        jdbcTemplate.update("DELETE FROM team_metric_snapshots");
        jdbcTemplate.update("DELETE FROM sentiment_risk_assessments");
        jdbcTemplate.update("DELETE FROM match_context_factors");
        jdbcTemplate.update("DELETE FROM import_item_mappings");
        jdbcTemplate.update("DELETE FROM data_dictionaries");
        jdbcTemplate.update("DELETE FROM review_lessons");
        jdbcTemplate.update("DELETE FROM post_match_reviews");
        jdbcTemplate.update("DELETE FROM bets");
        jdbcTemplate.update("DELETE FROM bet_plan_items");
        jdbcTemplate.update("DELETE FROM bet_plans");
        jdbcTemplate.update("DELETE FROM analysis_reports");
        jdbcTemplate.update("DELETE FROM odds_selection_snapshots");
        jdbcTemplate.update("DELETE FROM odds_market_snapshots");
        jdbcTemplate.update("DELETE FROM odds_snapshots");
        jdbcTemplate.update("DELETE FROM data_conflicts");
        jdbcTemplate.update("DELETE FROM source_evidence");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_player_stats");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        itemRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void pendingItemCannotBeImported() {
        ImportItem item = saveItem(ImportItemType.ANALYSIS, ImportItemStatus.PENDING_REVIEW, true,
                "{\"id\":\"a1\",\"match\":\"A vs B\",\"matchday\":\"2026-06-22\"}");

        assertThatThrownBy(() -> service.importItem(item.getId(), "admin"))
                .hasMessageContaining("只有已批准");
        assertThat(count("matches")).isZero();
    }

    @Test
    void approvedButInvalidJsonCannotBeImported() {
        ImportItem item = saveItem(ImportItemType.ANALYSIS, ImportItemStatus.APPROVED, false,
                "{\"id\":\"invalid-approved\",\"match\":\"A vs B\"}");

        assertThatThrownBy(() -> service.importItem(item.getId(), "admin"))
                .hasMessageContaining("只有已批准");
        assertThat(count("matches")).isZero();
        assertThat(count("import_item_mappings")).isZero();
    }

    @Test
    void approvedAnalysisImportsMatchAndReportAndIsIdempotent() {
        ImportItem item = saveItem(ImportItemType.ANALYSIS, ImportItemStatus.APPROVED, true,
                "{\"id\":\"analysis-1\",\"match\":\"西班牙 vs 沙特\",\"matchday\":\"2026-06-22\",\"jc_code\":\"周日037\",\"conclusion_type\":\"盘口判断\",\"confidence\":\"中\",\"risks\":[\"首发未核\"],\"recommended\":[{\"type\":\"HHAD\"}],\"dimensions\":{\"form\":\"ok\"},\"sources\":[{\"name\":\"FIFA\",\"url\":\"https://example.test\"}],\"narrative_md\":\"正文\"}");

        CoreDataImportResponse first = service.importItem(item.getId(), "admin");
        CoreDataImportResponse second = service.importItem(item.getId(), "admin");

        assertThat(first.mappings()).isNotEmpty();
        assertThat(second.mappings()).hasSameSizeAs(first.mappings());
        assertThat(count("matches")).isEqualTo(1);
        assertThat(count("analysis_reports")).isEqualTo(1);
        assertThat(count("source_evidence")).isEqualTo(1);
    }

    @Test
    void approvedTeamPlayerAndMatchImportsMasterDataAndIsIdempotent() {
        ImportItem team = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France","fifa_code":"FRA","country_iso2":"FR","flag_asset_key":"fr","confederation":"UEFA","group_name":"D组","metadata_source_ref":"FIFA team directory"}}
                """);
        ImportItem player = saveItem(ImportItemType.PLAYER, ImportItemStatus.APPROVED, true,
                """
                {"type":"PLAYER","payload":{"player_key":"france-10","team_key":"france","display_name":"France 10","shirt_number":10}}
                """);
        ImportItem match = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26","home_team_key":"france"}}
                """);

        assertThat(service.importItem(team.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(player.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(match.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(match.getId(), "admin").mappings()).hasSize(1);

        assertThat(count("teams")).isEqualTo(1);
        assertThat(count("players")).isEqualTo(1);
        assertThat(count("matches")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams WHERE team_key='france' AND fifa_code='FRA'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT country_iso2 FROM teams WHERE team_key='france'", String.class)).isEqualTo("FR");
        assertThat(jdbcTemplate.queryForObject("SELECT flag_asset_key FROM teams WHERE team_key='france'", String.class)).isEqualTo("fr");
        assertThat(jdbcTemplate.queryForObject("SELECT confederation FROM teams WHERE team_key='france'", String.class)).isEqualTo("UEFA");
        assertThat(jdbcTemplate.queryForObject("SELECT group_name FROM teams WHERE team_key='france'", String.class)).isEqualTo("D组");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM players WHERE player_key='france-10' AND team_id IS NOT NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE match_key='20260626-france-brazil' AND home_team_id IS NOT NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void approvedLineupEventAndStatsImportMatchDetailRows() {
        ImportItem team = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France"}}
                """);
        ImportItem player = saveItem(ImportItemType.PLAYER, ImportItemStatus.APPROVED, true,
                """
                {"type":"PLAYER","payload":{"player_key":"france-10","team_key":"france","display_name":"France 10"}}
                """);
        ImportItem match = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26"}}
                """);
        service.importItem(team.getId(), "admin");
        service.importItem(player.getId(), "admin");
        service.importItem(match.getId(), "admin");

        ImportItem lineup = saveItem(ImportItemType.MATCH_LINEUP, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH_LINEUP","payload":{"match_key":"20260626-france-brazil","team_key":"france","player_key":"france-10","role":"STARTER","position":"LW","is_starter":true}}
                """);
        ImportItem event = saveItem(ImportItemType.MATCH_EVENT, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH_EVENT","payload":{"match_key":"20260626-france-brazil","team_key":"france","player_key":"france-10","event_minute":12,"event_type":"GOAL"}}
                """);
        ImportItem stats = saveItem(ImportItemType.MATCH_STATS, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH_STATS","payload":{"match_key":"20260626-france-brazil","team_key":"france","stats_type":"FULL_TIME","goals_for":2,"goals_against":1}}
                """);

        assertThat(service.importItem(lineup.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(event.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(stats.getId(), "admin").mappings()).hasSize(1);
        assertThat(count("match_lineups")).isEqualTo(1);
        assertThat(count("match_events")).isEqualTo(1);
        assertThat(count("match_team_stats")).isEqualTo(1);
    }

    @Test
    void matchDetailWithMissingProvidedAssociationFailsClearly() {
        ImportItem match = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26"}}
                """);
        service.importItem(match.getId(), "admin");

        ImportItem lineup = saveItem(ImportItemType.MATCH_LINEUP, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH_LINEUP","payload":{"match_key":"20260626-france-brazil","team_key":"missing-team","role":"STARTER"}}
                """);

        assertThatThrownBy(() -> service.importItem(lineup.getId(), "admin"))
                .hasMessageContaining("team_key does not exist for lineup: missing-team");
        assertThat(count("match_lineups")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_item_mappings WHERE target_type='MATCH_LINEUP'", Integer.class)).isZero();
    }

    @Test
    void approvedStandaloneDecisionAndReviewTypesImportBusinessRows() {
        ImportItem team = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France"}}
                """);
        ImportItem match = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26","home_team_key":"france"}}
                """);
        service.importItem(team.getId(), "admin");
        service.importItem(match.getId(), "admin");

        ImportItem plan = saveItem(ImportItemType.BET_PLAN, ImportItemStatus.APPROVED, true,
                """
                {"type":"BET_PLAN","payload":{"match_key":"20260626-france-brazil","plan_key":"plan-france-brazil","title":"France plan","budget_amount":"200","items":[{"market_type":"HAD","selection":"HOME","stake_suggestion":"100","odds":"1.95"},{"market_type":"TTG","selection":"2 goals","stake_suggestion":"40","odds":"3.10"}]}}
                """);
        ImportItem bet = saveItem(ImportItemType.BET, ImportItemStatus.APPROVED, true,
                """
                {"type":"BET","payload":{"bet_id":"ticket-001-1","ticket_no":"TICKET-001","match_key":"20260626-france-brazil","match":"France vs Brazil","matchday":"2026-06-26","market_type":"HAD","selection":"HOME","stake":"100","odds":"1.95"}}
                """);
        ImportItem review = saveItem(ImportItemType.POST_REVIEW, ImportItemStatus.APPROVED, true,
                """
                {"type":"POST_REVIEW","payload":{"match_key":"20260626-france-brazil","review_key":"review-france-brazil","title":"France vs Brazil review","overall_summary":"logic works"}}
                """);
        ImportItem lesson = saveItem(ImportItemType.REVIEW_LESSON, ImportItemStatus.APPROVED, true,
                """
                {"type":"REVIEW_LESSON","payload":{"review_key":"review-france-brazil","lesson_type":"CLV","lesson_text":"track closing odds","severity":"INFO"}}
                """);

        assertThat(service.importItem(plan.getId(), "admin").mappings()).hasSize(3);
        assertThat(service.importItem(bet.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(review.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(lesson.getId(), "admin").mappings()).hasSize(1);
        assertThat(count("bet_plans")).isEqualTo(1);
        assertThat(count("bet_plan_items")).isEqualTo(2);
        assertThat(count("bets")).isEqualTo(1);
        assertThat(count("post_match_reviews")).isEqualTo(1);
        assertThat(count("review_lessons")).isEqualTo(1);
    }

    @Test
    void envelopeAnalysisOddsAndSourceImportPayloadContent() {
        ImportItem analysis = saveItem(ImportItemType.ANALYSIS, ImportItemStatus.APPROVED, true,
                """
                {"type":"ANALYSIS","idempotency_key":"analysis-envelope-1","source":{"name":"codex"},"payload":{"id":"analysis-envelope-1","match":"France vs Brazil","matchday":"2026-06-26","jc_code":"001","conclusion_type":"PAYLOAD_CONCLUSION","confidence":"HIGH","sources":[{"name":"Payload Source","url":"https://example.test","summary":"payload evidence"}],"narrative_md":"payload narrative"}}
                """);
        ImportItem odds = saveItem(ImportItemType.ODDS, ImportItemStatus.APPROVED, true,
                """
                {"type":"ODDS","idempotency_key":"odds-envelope-1","source":{"name":"vendor"},"payload":{"event_id":"odds-envelope-1","match":"France vs Brazil","matchday":"2026-06-26","markets":[{"bookmaker":"PayloadBook","market":"HAD","selections":[{"code":"HOME","name":"Home","odds":"1.90"},{"code":"AWAY","name":"Away","odds":"4.10"}]}]}}
                """);
        ImportItem source = saveItem(ImportItemType.SOURCE, ImportItemStatus.APPROVED, true,
                """
                {"type":"SOURCE","idempotency_key":"source-envelope-1","source":{"name":"scout"},"payload":{"id":"source-envelope-1","match":"France vs Brazil","matchday":"2026-06-26","snapshots":[{"type":"INJURY","name":"Payload Team News","summary":"payload source summary"}],"conflicts":[{"type":"LINEUP","entity":"france","field":"status","current":"unknown","incoming":"fit"}]}}
                """);

        assertThat(service.importItem(analysis.getId(), "admin").mappings()).hasSize(2);
        assertThat(service.importItem(odds.getId(), "admin").mappings()).hasSize(1);
        assertThat(service.importItem(source.getId(), "admin").mappings()).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analysis_reports WHERE analysis_id='analysis-envelope-1' AND conclusion_type='PAYLOAD_CONCLUSION' AND confidence='HIGH'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_evidence WHERE source_name='Payload Source' AND summary='payload evidence'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_market_snapshots WHERE bookmaker='PayloadBook' AND market_code='HAD'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='HOME' AND selection_name='Home' AND odds_value=1.9000", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_evidence WHERE source_name='Payload Team News' AND summary='payload source summary'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM data_conflicts WHERE conflict_type='LINEUP' AND entity_key='france'", Integer.class)).isEqualTo(1);
    }

    @Test
    void sourceJsonImportsExtendedFootballIntelligenceSignalsAndMetrics() {
        service.importItem(saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"法国","fifa_code":"FRA","country_iso2":"FR","flag_asset_key":"fr"}}
                """).getId(), "admin");
        service.importItem(saveItem(ImportItemType.PLAYER, ImportItemStatus.APPROVED, true,
                """
                {"type":"PLAYER","payload":{"player_key":"france-10","team_key":"france","display_name":"姆巴佩","shirt_number":10}}
                """).getId(), "admin");
        service.importItem(saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"法国 vs 巴西","matchday":"2026-06-26","home_team_key":"france"}}
                """).getId(), "admin");

        ImportItem source = saveItem(ImportItemType.SOURCE, ImportItemStatus.APPROVED, true,
                """
                {
                  "type":"SOURCE",
                  "payload":{
                    "match_key":"20260626-france-brazil",
                    "match":"法国 vs 巴西",
                    "matchday":"2026-06-26",
                    "weather":{"type":"TEMPERATURE","title":"天气","summary":"气温适中","source_name":"天气源"},
                    "referee":{"type":"CARD_TENDENCY","title":"裁判","summary":"黄牌偏多","source_name":"裁判源"},
                    "travel_rest":{"type":"REST_DAYS","title":"休息","summary":"法国休息 5 天","source_name":"赛程源"},
                    "rotation":{"type":"GROUP_QUALIFICATION","title":"轮换","summary":"小组出线压力下预计少轮换","source_name":"积分形势"},
                    "press_conference":{"type":"OFFICIAL_TEAM_NEWS","title":"发布会","summary":"主帅确认核心参加合练","source_name":"发布会源"},
                    "public_sentiment":{"type":"MEDIA_HEAT","title":"公众舆情","summary":"社媒讨论集中在法国边路","source_name":"媒体源"},
                    "tactical_matchup":{"type":"TACTIC","title":"战术对位","summary":"法国左路速度对巴西右路保护形成考验","source_name":"战术源"},
                    "injuries":[{"type":"PLAYER_INJURY","title":"伤停","summary":"核心健康","entity_key":"france-10","source_name":"发布会"}],
                    "market_signals":[{"market":"MATCH_WIN","bookmaker":"竞彩","opening_odds":"2.10","current_odds":"2.00","implied_probability":"0.5000","public_bet_pct":"58.5","movement_direction":"HOME_SHORTENING","source_name":"市场源"}],
                    "team_metrics":[{"team_key":"france","period_key":"last_5","metric_type":"RECENT_FORM","xg":"8.4","xga":"4.1","ppda":"9.8","form_score":"82","source_name":"技术源"}],
                    "player_metrics":[{"player_key":"france-10","team_key":"france","period_key":"last_3","metric_type":"RECENT_FORM","minutes_played":"252","goals":"2","xg":"1.9","availability_score":"95","expected_starting_probability":"0.88","source_name":"球员源"}]
                  }
                }
                """);

        CoreDataImportResponse response = service.importItem(source.getId(), "admin");

        assertThat(response.mappings()).anySatisfy(mapping -> assertThat(mapping.targetType()).isEqualTo("TEAM_METRIC_SNAPSHOT"));
        assertThat(response.mappings()).anySatisfy(mapping -> assertThat(mapping.targetType()).isEqualTo("PLAYER_METRIC_SNAPSHOT"));
        assertThat(response.mappings()).anySatisfy(mapping -> assertThat(mapping.targetType()).isEqualTo("MATCH_MARKET_SIGNAL"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category IN ('WEATHER','REFEREE','TRAVEL','ROTATION','PRESS_CONFERENCE','PUBLIC_OPINION','TACTICAL_MATCHUP','INJURY','MARKET')", Integer.class)).isGreaterThanOrEqualTo(9);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category IN ('TRAVEL_REST','MARKET_SIGNAL','PUBLIC_SENTIMENT')", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT xg FROM team_metric_snapshots WHERE team_id=(SELECT id FROM teams WHERE team_key='france')", java.math.BigDecimal.class))
                .isEqualByComparingTo("8.4000");
        assertThat(jdbcTemplate.queryForObject("SELECT expected_starting_probability FROM player_metric_snapshots WHERE player_id=(SELECT id FROM players WHERE player_key='france-10')", java.math.BigDecimal.class))
                .isEqualByComparingTo("0.880000");
        assertThat(jdbcTemplate.queryForObject("SELECT public_bet_pct FROM match_market_signals", java.math.BigDecimal.class))
                .isEqualByComparingTo("58.5000");
    }

    @Test
    void partialMasterDataUpdateDoesNotClearExistingAuthoritativeFields() {
        ImportItem france = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France","fifa_code":"FRA","attack_profile":"attack","defense_profile":"defense"}}
                """);
        ImportItem brazil = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"brazil","display_name":"Brazil","fifa_code":"BRA"}}
                """);
        service.importItem(france.getId(), "admin");
        service.importItem(brazil.getId(), "admin");
        ImportItem playerFull = saveItem(ImportItemType.PLAYER, ImportItemStatus.APPROVED, true,
                """
                {"type":"PLAYER","payload":{"player_key":"france-10","team_key":"france","display_name":"France 10","shirt_number":10,"position":"FW","status":"AVAILABLE"}}
                """);
        ImportItem matchFull = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26","jc_code":"001","competition":"World Cup","stage":"Group","venue":"Lusail","kickoff_time":"2026-06-26T20:00:00","home_team_key":"france","away_team_key":"brazil","status":"SCHEDULED","result_status":"UNKNOWN"}}
                """);
        service.importItem(playerFull.getId(), "admin");
        service.importItem(matchFull.getId(), "admin");

        service.importItem(saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France Team"}}
                """).getId(), "admin");
        service.importItem(saveItem(ImportItemType.PLAYER, ImportItemStatus.APPROVED, true,
                """
                {"type":"PLAYER","payload":{"player_key":"france-10","display_name":"France Ten"}}
                """).getId(), "admin");
        service.importItem(saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France Team vs Brazil Team"}}
                """).getId(), "admin");

        assertThat(jdbcTemplate.queryForObject("SELECT display_name FROM teams WHERE team_key='france'", String.class)).isEqualTo("France Team");
        assertThat(jdbcTemplate.queryForObject("SELECT fifa_code FROM teams WHERE team_key='france'", String.class)).isEqualTo("FRA");
        assertThat(jdbcTemplate.queryForObject("SELECT attack_profile FROM teams WHERE team_key='france'", String.class)).isEqualTo("attack");
        assertThat(jdbcTemplate.queryForObject("SELECT shirt_number FROM players WHERE player_key='france-10'", Integer.class)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("SELECT position FROM players WHERE player_key='france-10'", String.class)).isEqualTo("FW");
        assertThat(jdbcTemplate.queryForObject("SELECT team_id IS NOT NULL FROM players WHERE player_key='france-10'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT competition FROM matches WHERE match_key='20260626-france-brazil'", String.class)).isEqualTo("World Cup");
        assertThat(jdbcTemplate.queryForObject("SELECT venue FROM matches WHERE match_key='20260626-france-brazil'", String.class)).isEqualTo("Lusail");
        assertThat(jdbcTemplate.queryForObject("SELECT home_team_id IS NOT NULL FROM matches WHERE match_key='20260626-france-brazil'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT away_team_id IS NOT NULL FROM matches WHERE match_key='20260626-france-brazil'", Boolean.class)).isTrue();
    }

    @Test
    void duplicateApprovedItemsWithSameBusinessKeysReuseExistingRows() {
        ImportItem team = saveItem(ImportItemType.TEAM, ImportItemStatus.APPROVED, true,
                """
                {"type":"TEAM","payload":{"team_key":"france","display_name":"France"}}
                """);
        ImportItem match = saveItem(ImportItemType.MATCH, ImportItemStatus.APPROVED, true,
                """
                {"type":"MATCH","payload":{"match_key":"20260626-france-brazil","match_name":"France vs Brazil","matchday":"2026-06-26","home_team_key":"france"}}
                """);
        service.importItem(team.getId(), "admin");
        service.importItem(match.getId(), "admin");

        String planJson = """
                {"type":"BET_PLAN","payload":{"match_key":"20260626-france-brazil","plan_key":"plan-france-brazil","title":"France plan","items":[{"market_type":"HAD","selection":"HOME","stake_suggestion":"100","odds":"1.95"}]}}
                """;
        String betJson = """
                {"type":"BET","payload":{"bet_id":"ticket-001-1","ticket_no":"TICKET-001","match_key":"20260626-france-brazil","match":"France vs Brazil","matchday":"2026-06-26","market_type":"HAD","selection":"HOME","stake":"100","odds":"1.95"}}
                """;
        String reviewJson = """
                {"type":"POST_REVIEW","payload":{"match_key":"20260626-france-brazil","review_key":"review-france-brazil","title":"France review","overall_summary":"ok"}}
                """;

        ImportItem plan1 = saveItem(ImportItemType.BET_PLAN, ImportItemStatus.APPROVED, true, planJson);
        ImportItem plan2 = saveItem(ImportItemType.BET_PLAN, ImportItemStatus.APPROVED, true, planJson);
        ImportItem bet1 = saveItem(ImportItemType.BET, ImportItemStatus.APPROVED, true, betJson);
        ImportItem bet2 = saveItem(ImportItemType.BET, ImportItemStatus.APPROVED, true, betJson);
        ImportItem review1 = saveItem(ImportItemType.POST_REVIEW, ImportItemStatus.APPROVED, true, reviewJson);
        ImportItem review2 = saveItem(ImportItemType.POST_REVIEW, ImportItemStatus.APPROVED, true, reviewJson);

        service.importItem(plan1.getId(), "admin");
        service.importItem(plan2.getId(), "admin");
        service.importItem(bet1.getId(), "admin");
        service.importItem(bet2.getId(), "admin");
        service.importItem(review1.getId(), "admin");
        service.importItem(review2.getId(), "admin");

        assertThat(count("bet_plans")).isEqualTo(1);
        assertThat(count("bet_plan_items")).isEqualTo(1);
        assertThat(count("bets")).isEqualTo(1);
        assertThat(count("post_match_reviews")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_item_mappings WHERE target_type IN ('BET_PLAN','BET_PLAN_ITEM','BET','POST_MATCH_REVIEW')", Integer.class)).isEqualTo(8);
    }

    @Test
    void successfulCoreImportMovesPendingFileToImportedDateAndAvoidsRescan() throws Exception {
        Path inboxRoot = tempDir.resolve("data-inbox");
        Path pending = inboxRoot.resolve("pending");
        Files.createDirectories(pending);
        Path source = pending.resolve("team.json");
        Files.writeString(source, """
                {"type":"TEAM","idempotency_key":"team-france","source":{"name":"test"},"payload":{"team_key":"france","display_name":"France"}}
                """);

        reviewService.scanAndPersist(pending);
        ImportItem item = itemRepository.findAll().get(0);
        reviewService.approve(item.getId(), "admin");
        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        Path archived = inboxRoot.resolve("imported").resolve(LocalDate.now().toString()).resolve("team.json");
        assertThat(response.message()).contains("Source JSON archived to");
        assertThat(Files.exists(source)).isFalse();
        assertThat(Files.exists(archived)).isTrue();
        assertThat(reviewService.dryRun(pending).totalItems()).isZero();
    }

    @Test
    void rejectedImportItemMovesPendingFileToRejectedDateAndKeepsReason() throws Exception {
        Path inboxRoot = tempDir.resolve("data-inbox");
        Path pending = inboxRoot.resolve("pending");
        Files.createDirectories(pending);
        Path source = pending.resolve("reject-team.json");
        Files.writeString(source, """
                {"type":"TEAM","idempotency_key":"team-reject","source":{"name":"test"},"payload":{"team_key":"reject","display_name":"Rejected Team"}}
                """);

        reviewService.scanAndPersist(pending);
        ImportItem item = itemRepository.findAll().get(0);
        reviewService.reject(item.getId(), "source not enough", "admin");

        Path archived = inboxRoot.resolve("rejected").resolve(LocalDate.now().toString()).resolve("reject-team.json");
        ImportItem rejected = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(Files.exists(source)).isFalse();
        assertThat(Files.exists(archived)).isTrue();
        assertThat(rejected.getStatus()).isEqualTo(ImportItemStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("source not enough");
    }

    @Test
    void approvedAnalysisImportsBetPlanAndPostMatchReview() {
        ImportItem item = saveItem(ImportItemType.ANALYSIS, ImportItemStatus.APPROVED, true,
                """
                {
                  "id":"analysis-plan-1",
                  "match":"法国 vs 巴西",
                  "matchday":"2026-06-26",
                  "jc_code":"周五001",
                  "conclusion_type":"盘口判断",
                  "confidence":"中高",
                  "risks":["临场首发未核"],
                  "recommended":[{"type":"HAD"}],
                  "bet_plan":{
                    "plan_key":"analysis-plan-1",
                    "title":"法国方向组合",
                    "conclusion_type":"盘口判断",
                    "confidence":"MEDIUM_HIGH",
                    "budget_amount":"100",
                    "risk_summary":"热门方向需控制投入",
                    "generated_by":"codex",
                    "generated_at":"2026-06-22T12:00:00",
                    "betting_method":"AI_VALUE_SPLIT",
                    "strategy_type":"主线加保险",
                    "items":[
                      {"market_type":"HAD","selection":"主胜","stake_suggestion":"60","odds":"1.85","logic_type":"MAIN","risk_level":"MEDIUM","line_value":"0","play_type":"单关"},
                      {"market_type":"TTG","selection":"2球","stake_suggestion":"20","odds":"3.20","logic_type":"LOW_SCORE","risk_level":"LOW","play_type":"总进球"}
                    ]
                  },
                  "post_match_review":{
                    "review_key":"review-france-brazil",
                    "title":"法国 vs 巴西复盘",
                    "math_review":"命中主线，返还覆盖投入",
                    "football_review":"法国边路压制有效",
                    "handicap_review":"盘口未被卡线",
                    "tournament_temperament_review":"大赛稳定性体现",
                    "odds_value_review":"入场赔率优于收盘",
                    "overall_summary":"判断符合赛前逻辑",
                    "lessons":[{"type":"CLV","text":"继续记录入场与收盘差","severity":"MEDIUM"}]
                  }
                }
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(6);
        assertThat(count("analysis_reports")).isEqualTo(1);
        assertThat(count("bet_plans")).isEqualTo(1);
        assertThat(count("bet_plan_items")).isEqualTo(2);
        assertThat(count("post_match_reviews")).isEqualTo(1);
        assertThat(count("review_lessons")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bet_plans WHERE plan_key='analysis-plan-1' AND status='IMPORTED' AND betting_method='AI_VALUE_SPLIT'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bet_plan_items WHERE market_type='HAD' AND selection_text='主胜' AND stake_suggestion=60.0000 AND play_type='单关'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM review_lessons WHERE lesson_type='CLV' AND severity='MEDIUM'", Integer.class)).isEqualTo(1);
    }


    @Test
    void approvedOddsImportsCompanyMarketsAndAllBooks() {
        ImportItem item = saveItem(ImportItemType.ODDS, ImportItemStatus.APPROVED, true,
                """
                {"match":"法国 vs 巴西","date":"2026-06-23","jc_code":"周一001","companies":[{"name":"Bet365","markets":[{"market":"HAD","odds":"1.80"},{"market":"HHAD","odds":"2.10"}]}],"all_books":[{"bookmaker":"Pinnacle","market":"HAD","odds":"1.83"}]}
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(3);
        assertThat(count("matches")).isEqualTo(1);
        assertThat(count("odds_snapshots")).isEqualTo(3);
    }


    @Test
    void approvedOddsStoresEveryMarketAndSelectionOdds() {
        ImportItem item = saveItem(ImportItemType.ODDS, ImportItemStatus.APPROVED, true,
                """
                {
                  "match":"法国 vs 巴西",
                  "date":"2026-06-23",
                  "jc_code":"周一001",
                  "companies":[{
                    "name":"Pinnacle",
                    "markets":[
                      {"market":"HAD","market_name":"胜平负","snapshot_type":"OPEN","captured_at":"2026-06-22T10:00:00","selections":[
                        {"code":"HOME","name":"主胜","odds":"1.80"},
                        {"code":"DRAW","name":"平","odds":"3.40"},
                        {"code":"AWAY","name":"客胜","odds":"4.20"}
                      ]},
                      {"market":"TTG","market_name":"总进球","snapshot_type":"LIVE","odds":{"0":"8.00","1":"4.50","7+":"12.00"}}
                    ]
                  }]
                }
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(2);
        assertThat(count("odds_snapshots")).isEqualTo(2);
        assertThat(count("odds_market_snapshots")).isEqualTo(2);
        assertThat(count("odds_selection_snapshots")).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='HOME' AND selection_name='主胜' AND odds_value=1.8000", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='7+' AND selection_name='7+' AND odds_value=12.0000", Integer.class)).isEqualTo(1);
    }

    @Test
    void approvedOddsStoresNestedObjectOddsValues() {
        ImportItem item = saveItem(ImportItemType.ODDS, ImportItemStatus.APPROVED, true,
                """
                {
                  "match":"法国 vs 巴西",
                  "date":"2026-06-23",
                  "jc_code":"周一001",
                  "markets":[{
                    "bookmaker":"Bet365",
                    "market":"HAD",
                    "market_name":"胜平负",
                    "odds":{
                      "HOME":{"name":"主胜","odds":"1.80","status":"OPEN"},
                      "DRAW":{"name":"平","price":"3.40"},
                      "AWAY":{"name":"客胜","value":"4.20"}
                    }
                  }]
                }
                """);

        service.importItem(item.getId(), "admin");

        assertThat(count("odds_market_snapshots")).isEqualTo(1);
        assertThat(count("odds_selection_snapshots")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='HOME' AND selection_name='主胜' AND odds_value=1.8000 AND selection_status='OPEN'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='DRAW' AND selection_name='平' AND odds_value=3.4000", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM odds_selection_snapshots WHERE selection_code='AWAY' AND selection_name='客胜' AND odds_value=4.2000", Integer.class)).isEqualTo(1);
    }


    @Test
    void approvedSourceImportsSentimentFactorsAndRiskAssessments() {
        ImportItem item = saveItem(ImportItemType.SOURCE, ImportItemStatus.APPROVED, true,
                """
                {
                  "match":"德国 vs 日本",
                  "date":"2026-06-24",
                  "jc_code":"周三001",
                  "external_factors":[{
                    "category":"WEATHER",
                    "type":"RAIN",
                    "title":"预计小雨",
                    "summary":"赛前两小时有小雨，草皮可能偏滑。",
                    "impact_direction":"MIXED",
                    "evidence_level":"DATA_VENDOR",
                    "source_name":"Weather Provider",
                    "source_url":"https://example.test/weather",
                    "reliability":"8.0",
                    "confidence":"7.5",
                    "observed_at":"2026-06-24T16:00:00",
                    "expires_at":"2026-06-24T19:00:00"
                  }],
                  "sentiment_records":[{
                    "category":"PUBLIC_SENTIMENT",
                    "type":"MEDIA_HEAT",
                    "title":"热门方舆论过热",
                    "summary":"主流预测集中支持德国大胜。",
                    "impact_direction":"NEGATIVE",
                    "evidence_level":"MAIN_MEDIA",
                    "source_name":"Media Digest",
                    "risks":[{"type":"PUBLIC_OVERHEAT","level":"HIGH","score":"78","title":"舆论过热","rationale":"热门预期过度集中","suggested_action":"LOWER_CONFIDENCE"}]
                  }]
                }
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(4);
        assertThat(count("match_context_factors")).isEqualTo(2);
        assertThat(count("sentiment_risk_assessments")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='WEATHER' AND factor_type='RAIN' AND title='预计小雨'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='PUBLIC_OPINION' AND impact_direction='NEGATIVE' AND source_name='Media Digest'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE risk_type='PUBLIC_OVERHEAT' AND risk_level='HIGH' AND risk_score=78.0000", Integer.class)).isEqualTo(1);
    }

    @Test
    void approvedSourceImportsAllSentimentAliasArraysWhenPresentTogether() {
        ImportItem item = saveItem(ImportItemType.SOURCE, ImportItemStatus.APPROVED, true,
                """
                {
                  "match":"法国 vs 巴西",
                  "date":"2026-06-25",
                  "external_factors":[{"category":"WEATHER","type":"RAIN","title":"小雨"}],
                  "factors":[{"category":"VENUE","type":"PITCH","title":"草皮偏软"}],
                  "sentiment_records":[{"type":"MEDIA_HEAT","title":"媒体热度升高"}],
                  "sentiments":[{"type":"LOCKER_ROOM","title":"更衣室稳定"}],
                  "risk_assessments":[{"type":"SCHEDULE_FATIGUE","level":"MEDIUM","score":"55","title":"赛程疲劳"}],
                  "risks":[{"type":"NEWS_CONFLICT","level":"LOW","score":"25","title":"消息冲突"}]
                }
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(7);
        assertThat(count("match_context_factors")).isEqualTo(4);
        assertThat(count("sentiment_risk_assessments")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='VENUE' AND factor_type='PITCH'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='PUBLIC_OPINION' AND factor_type='LOCKER_ROOM'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE risk_type='NEWS_CONFLICT' AND risk_level='LOW'", Integer.class)).isEqualTo(1);
    }

    @Test
    void approvedSourceImportsEvidenceConflictsAndAliases() {
        ImportItem item = saveItem(ImportItemType.SOURCE, ImportItemStatus.APPROVED, true,
                """
                {"match":"德国 vs 日本","date":"2026-06-24","snapshots":[{"type":"INJURY","name":"Team News","summary":"主力恢复训练","reliability":"8.5"}],"conflicts":[{"type":"LINEUP","entity":"player-1","field":"status","current":"unknown","incoming":"fit"}],"aliases":{"Germany":"德国"}}
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(2);
        assertThat(count("matches")).isEqualTo(1);
        assertThat(count("source_evidence")).isEqualTo(1);
        assertThat(count("data_conflicts")).isEqualTo(1);
        assertThat(count("data_dictionaries")).isEqualTo(1);
    }

    @Test
    void approvedBetsImportsBetRows() {
        ImportItem item = saveItem(ImportItemType.BETS, ImportItemStatus.APPROVED, true,
                """
                {"bets":[{"bet_id":"b1","比赛":"阿根廷 vs 墨西哥","比赛日":"2026-06-25","玩法":"HAD","投注项":"胜","投注额":"100","赔率":"1.95","命中":"PENDING"},{"bet_id":"b2","比赛":"阿根廷 vs 墨西哥","比赛日":"2026-06-25","玩法":"HHAD","投注项":"让胜","投注额":"50","赔率":"2.30"}]}
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(2);
        assertThat(count("matches")).isEqualTo(1);
        assertThat(count("bets")).isEqualTo(2);
    }

    @Test
    void approvedBetsImportsSettlementAndClvFields() {
        ImportItem item = saveItem(ImportItemType.BETS, ImportItemStatus.APPROVED, true,
                """
                {"bets":[{
                  "bet_id":"b-clv-1",
                  "ticket_no":"T-001",
                  "match":"法国 vs 巴西",
                  "matchday":"2026-06-26",
                  "bet_date":"2026-06-22",
                  "market_type":"HAD",
                  "selection":"主胜",
                  "stake":"60",
                  "odds":"1.95",
                  "closing_odds":"1.80",
                  "return_amount":"108",
                  "profit_loss":"48",
                  "hit_status":"HIT",
                  "settled_at":"2026-06-26T23:00:00",
                  "review_status":"READY"
                }]}
                """);

        CoreDataImportResponse response = service.importItem(item.getId(), "admin");

        assertThat(response.mappings()).hasSize(1);
        assertThat(count("bets")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bets WHERE ticket_no='T-001' AND bet_date='2026-06-22' AND matchday='2026-06-26'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bets WHERE closing_odds=1.8000 AND return_amount=108.0000 AND profit_loss=48.0000", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bets WHERE clv=0.083333 AND review_status='READY'", Integer.class)).isEqualTo(1);
    }

    private ImportItem saveItem(ImportItemType type, ImportItemStatus status, boolean validJson, String rawJson) {
        ImportJob job = new ImportJob();
        job.setArchivePath("test/archive");
        job.setStatus("SCANNED");
        job.setMessage("test");
        job.setTotalItems(1);
        job.setValidItems(validJson ? 1 : 0);
        job.setInvalidItems(validJson ? 0 : 1);

        ImportItem item = new ImportItem();
        item.setJob(job);
        item.setItemType(type);
        item.setStatus(status);
        item.setRelativePath(type.name().toLowerCase() + ".json");
        item.setSha256("0".repeat(64));
        item.setSummaryTitle("test");
        item.setValidJson(validJson);
        item.setValidationMessage(validJson ? "ok" : "invalid");
        item.setRawJson(rawJson);
        job.addItem(item);
        return jobRepository.save(job).getItems().get(0);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
