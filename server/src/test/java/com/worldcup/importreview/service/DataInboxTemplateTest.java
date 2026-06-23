package com.worldcup.importreview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataInboxTemplateTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void templatesAreParseableAndUseRequiredEnvelope() throws Exception {
        Path templateDir = Path.of("..", "data-inbox", "templates").toAbsolutePath().normalize();
        Map<String, String> expectedTypes = Map.of(
                "team.json", "TEAM",
                "player.json", "PLAYER",
                "match.json", "MATCH",
                "odds.json", "ODDS",
                "analysis.json", "ANALYSIS",
                "bet-plan.json", "BET_PLAN",
                "post-review.json", "POST_REVIEW"
        );

        for (Map.Entry<String, String> entry : expectedTypes.entrySet()) {
            Path template = templateDir.resolve(entry.getKey());
            assertThat(template).exists();

            JsonNode root = objectMapper.readTree(Files.readString(template));
            assertThat(root.path("type").asText()).isEqualTo(entry.getValue());
            assertThat(root.path("idempotency_key").asText()).isNotBlank();
            assertThat(root.path("source").isObject()).isTrue();
            assertThat(root.path("payload").isObject()).isTrue();
        }
    }
}
