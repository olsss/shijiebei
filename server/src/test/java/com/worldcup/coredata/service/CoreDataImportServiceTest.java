package com.worldcup.coredata.service;

import com.worldcup.coredata.api.dto.CoreDataImportResponse;
import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import com.worldcup.importreview.domain.ImportJob;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.repo.ImportJobRepository;
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
    void clean() {
        jdbcTemplate.update("DELETE FROM import_item_mappings");
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
