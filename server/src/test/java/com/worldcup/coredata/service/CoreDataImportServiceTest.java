package com.worldcup.coredata.service;

import com.worldcup.coredata.api.dto.CoreDataImportResponse;
import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import com.worldcup.importreview.domain.ImportJob;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.repo.ImportJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

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

    @BeforeEach
    @AfterEach
    void clean() {
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
        jdbcTemplate.update("DELETE FROM match_lineups");
        jdbcTemplate.update("DELETE FROM match_player_stats");
        jdbcTemplate.update("DELETE FROM match_team_stats");
        jdbcTemplate.update("DELETE FROM match_events");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
        jdbcTemplate.update("DELETE FROM matches");
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
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='PUBLIC_SENTIMENT' AND impact_direction='NEGATIVE' AND source_name='Media Digest'", Integer.class)).isEqualTo(1);
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
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM match_context_factors WHERE factor_category='PUBLIC_SENTIMENT' AND factor_type='LOCKER_ROOM'", Integer.class)).isEqualTo(1);
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

