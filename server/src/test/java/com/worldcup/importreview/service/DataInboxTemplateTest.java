package com.worldcup.importreview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DataInboxTemplateTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void templatesAreParseableAndUseRequiredEnvelope() throws Exception {
        Path templateDir = Path.of("..", "data-inbox", "templates").toAbsolutePath().normalize();
        assertThat(templateDir).isDirectory();

        try (Stream<Path> templates = Files.list(templateDir)) {
            var jsonTemplates = templates
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
            assertThat(jsonTemplates).isNotEmpty();

            for (Path template : jsonTemplates) {
                JsonNode root = objectMapper.readTree(Files.readString(template));
                assertThat(root.path("type").asText())
                        .as(template.getFileName() + " type")
                        .isIn("TEAM", "PLAYER", "MATCH", "ODDS", "ANALYSIS", "BET_PLAN", "POST_REVIEW", "SOURCE");
                assertThat(root.path("idempotency_key").asText())
                        .as(template.getFileName() + " idempotency_key")
                        .isNotBlank();
                assertThat(root.path("source").isObject())
                        .as(template.getFileName() + " source")
                        .isTrue();
                assertThat(root.path("payload").isObject())
                        .as(template.getFileName() + " payload")
                        .isTrue();
            }
        }
    }

    @Test
    void footballProfileTemplatesGuideTeamAndPlayerMetricCollection() throws Exception {
        Path templateDir = Path.of("..", "data-inbox", "templates").toAbsolutePath().normalize();

        JsonNode playerTemplate = objectMapper.readTree(Files.readString(templateDir.resolve("player.json")));
        assertThat(playerTemplate.path("payload").path("player_key").asText()).isEqualTo("france-10");

        JsonNode teamMetrics = objectMapper.readTree(Files.readString(templateDir.resolve("team-metrics-source.json")));
        assertThat(teamMetrics.path("type").asText()).isEqualTo("SOURCE");
        assertThat(teamMetrics.path("payload").path("team_metrics")).isNotEmpty();
        assertThat(teamMetrics.path("payload").path("team_news")).isNotEmpty();

        JsonNode playerAvailability = objectMapper.readTree(Files.readString(templateDir.resolve("player-availability-source.json")));
        assertThat(playerAvailability.path("type").asText()).isEqualTo("SOURCE");
        assertThat(playerAvailability.path("payload").path("player_metrics")).isNotEmpty();
        assertThat(playerAvailability.toString()).contains("expected_starting_probability");
        assertThat(playerAvailability.toString()).contains("france-10");

        JsonNode footballIntelligence = objectMapper.readTree(Files.readString(templateDir.resolve("source-football-intelligence.json")));
        JsonNode intelligencePayload = footballIntelligence.path("payload");
        assertThat(intelligencePayload.path("referee").isObject()).isTrue();
        assertThat(intelligencePayload.path("travel_rest").isObject()).isTrue();
        assertThat(intelligencePayload.path("press_conference").isObject()).isTrue();
        assertThat(intelligencePayload.path("fan_sentiment").isObject()).isTrue();
        assertThat(intelligencePayload.path("tactical_matchup").isObject()).isTrue();
        assertThat(intelligencePayload.path("market_signals")).isNotEmpty();
    }

}
