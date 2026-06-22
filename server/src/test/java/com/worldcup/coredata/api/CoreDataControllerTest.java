package com.worldcup.coredata.api;

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
class CoreDataControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ImportJobRepository jobRepository;

    @Autowired
    ImportItemRepository itemRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
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
    void overviewReturnsCoreDataCounts() throws Exception {
        mockMvc.perform(get("/api/core-data/overview").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matches").isNumber());
    }

    @Test
    void importApprovedItemAndReadMappings() throws Exception {
        ImportItem item = saveApprovedAnalysisItem();

        mockMvc.perform(post("/api/core-data/import-items/" + item.getId() + "/import")
                        .with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importItemId").value(item.getId()));

        mockMvc.perform(get("/api/core-data/import-items/" + item.getId() + "/mappings")
                        .with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].targetType").exists());
    }

    private ImportItem saveApprovedAnalysisItem() {
        ImportJob job = new ImportJob();
        job.setArchivePath("test/archive");
        job.setStatus("SCANNED");
        job.setMessage("test");
        job.setTotalItems(1);
        job.setValidItems(1);
        job.setInvalidItems(0);

        ImportItem item = new ImportItem();
        item.setJob(job);
        item.setItemType(ImportItemType.ANALYSIS);
        item.setStatus(ImportItemStatus.APPROVED);
        item.setRelativePath("analysis.json");
        item.setSha256("1".repeat(64));
        item.setSummaryTitle("西班牙 vs 沙特");
        item.setValidJson(true);
        item.setValidationMessage("ok");
        item.setRawJson("{\"id\":\"analysis-api-1\",\"match\":\"西班牙 vs 沙特\",\"matchday\":\"2026-06-22\",\"sources\":[{\"name\":\"FIFA\"}]}");
        job.addItem(item);
        return jobRepository.save(job).getItems().get(0);
    }
}


