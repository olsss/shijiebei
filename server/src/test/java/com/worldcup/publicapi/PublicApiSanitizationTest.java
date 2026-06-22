package com.worldcup.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApiSanitizationTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PublicApiMapper mapper = new PublicApiMapper();

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
    void sanitizedSummaryJsonDoesNotExposeForbiddenKeysOrValues() throws Exception {
        String unsafe = "ticketNo=ABC123 stake=88 reviewedBy=admin";
        String sanitized = mapper.sanitizeText(unsafe);
        var dto = new PublicDecisionReport(
                1L, 2L, "Spain vs Brazil", LocalDate.of(2026, 6, 23), "031",
                "VALUE", "HIGH", sanitized, sanitized, sanitized
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("ABC123", "stake=88", "reviewedBy=admin");
        assertThat(json).doesNotContain("ticketNo", "stake", "reviewedBy");
        assertThat(json).contains("[REDACTED]");
    }

    @Test
    void sanitizerRemovesSensitiveValuesFromFreeText() {
        String unsafe = "ticketNo=ABC123 stake=88 profitLoss=-20 C:/secret/archive.json {\"raw\":true} reviewedBy=admin";

        String sanitized = mapper.sanitizeText(unsafe);

        assertThat(sanitized).doesNotContain("ABC123", "stake=88", "profitLoss=-20", "C:/secret", "raw", "reviewedBy=admin");
        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    void sanitizerRemovesSnakeCaseAndQuotedJsonSensitiveTokens() {
        String unsafe = "ticket_no=ABC123 stake_suggestion=12 budget_amount=99 return_amount=180 "
                + "profit_loss=-20 raw_payload={\"ticketNo\":\"ABC123\",\"stake\":88}";

        String sanitized = mapper.sanitizeText(unsafe);

        assertThat(sanitized).doesNotContain("ticket_no", "stake_suggestion", "budget_amount", "return_amount");
        assertThat(sanitized).doesNotContain("profit_loss", "raw_payload", "ticketNo", "stake");
        assertThat(sanitized).doesNotContain("ABC123", "12", "99", "180", "-20", "88");
        assertThat(sanitized).contains("[REDACTED]");
    }
}
