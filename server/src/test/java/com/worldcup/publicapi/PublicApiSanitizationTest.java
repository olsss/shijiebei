package com.worldcup.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApiSanitizationTest {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    PublicApiMapper mapper = new PublicApiMapper();

    @Test
    void publicDtoJsonDoesNotExposeForbiddenFieldNames() throws Exception {
        var dto = new PublicDecisionReport(
                1L, 2L, "Spain vs Brazil", LocalDate.of(2026, 6, 23), "031",
                "VALUE", "HIGH", "risk summary", "review summary", "lesson summary"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("rawJson", "rawPayload", "payload", "archivePath", "sourcePath");
        assertThat(json).doesNotContain("ticketNo", "stake", "stakeSuggestion", "budgetAmount");
        assertThat(json).doesNotContain("returnAmount", "profitLoss", "approvedBy", "reviewedBy", "reviewNote");
        assertThat(json).doesNotContain("mappings", "importItemId");
    }

    @Test
    void sanitizerRemovesSensitiveValuesFromFreeText() {
        String unsafe = "ticketNo=ABC123 stake=88 profitLoss=-20 C:/secret/archive.json {\"raw\":true} reviewedBy=admin";

        String sanitized = mapper.sanitizeText(unsafe);

        assertThat(sanitized).doesNotContain("ABC123", "stake=88", "profitLoss=-20", "C:/secret", "raw", "reviewedBy=admin");
        assertThat(sanitized).contains("[REDACTED]");
    }
}
