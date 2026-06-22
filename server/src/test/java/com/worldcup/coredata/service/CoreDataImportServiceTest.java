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
        jdbcTemplate.update("DELETE FROM bets");
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

