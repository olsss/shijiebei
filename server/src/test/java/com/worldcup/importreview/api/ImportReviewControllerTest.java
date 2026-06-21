package com.worldcup.importreview.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.repo.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImportItemRepository itemRepository;

    @Autowired
    private ImportJobRepository jobRepository;

    @TempDir
    Path archiveDir;

    @BeforeEach
    void setUp() throws Exception {
        itemRepository.deleteAll();
        jobRepository.deleteAll();
        Files.createDirectories(archiveDir.resolve("analysis"));
        Files.createDirectories(archiveDir.resolve("odds"));
        Files.createDirectories(archiveDir.resolve("sources"));
        Files.writeString(archiveDir.resolve("bets.json"), "{\"bets\":[{\"bet_id\":\"b1\"}]}");
        Files.writeString(archiveDir.resolve("analysis/match.json"), "{\"id\":\"a1\",\"match\":\"A vs B\"}");
        Files.writeString(archiveDir.resolve("analysis/bad.json"), "{bad-json");
    }

    @Test
    void scanCreatesJobAndItems() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("archivePath", archiveDir.toString()));

        mockMvc.perform(post("/api/import-jobs/scan")
                        .with(httpBasic("admin", "admin123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.validItems").value(2))
                .andExpect(jsonPath("$.data.invalidItems").value(1));

        mockMvc.perform(get("/api/import-items")
                        .with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void approveValidItemAndRejectInvalidApproval() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("archivePath", archiveDir.toString()));
        mockMvc.perform(post("/api/import-jobs/scan")
                        .with(httpBasic("admin", "admin123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String itemsJson = mockMvc.perform(get("/api/import-items")
                        .with(httpBasic("admin", "admin123456")))
                .andReturn().getResponse().getContentAsString();
        var root = objectMapper.readTree(itemsJson).get("data");
        long validId = findItemIdByValidity(root, true);
        long invalidId = findItemIdByValidity(root, false);

        mockMvc.perform(post("/api/import-items/" + validId + "/approve")
                        .with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/import-items/" + invalidId + "/approve")
                        .with(httpBasic("admin", "admin123456")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectStoresReason() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("archivePath", archiveDir.toString()));
        mockMvc.perform(post("/api/import-jobs/scan")
                        .with(httpBasic("admin", "admin123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String itemsJson = mockMvc.perform(get("/api/import-items")
                        .with(httpBasic("admin", "admin123456")))
                .andReturn().getResponse().getContentAsString();
        long itemId = objectMapper.readTree(itemsJson).get("data").get(0).get("id").asLong();

        mockMvc.perform(post("/api/import-items/" + itemId + "/reject")
                        .with(httpBasic("admin", "admin123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"字段不完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    private long findItemIdByValidity(JsonNode items, boolean validJson) {
        for (JsonNode item : items) {
            if (item.get("validJson").asBoolean() == validJson) {
                return item.get("id").asLong();
            }
        }
        throw new AssertionError("未找到 validJson=" + validJson + " 的导入项");
    }
}
