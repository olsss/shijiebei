package com.worldcup.importreview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.importreview.domain.ImportItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JsonArchiveScannerTest {

    @TempDir
    Path archiveDir;

    @Test
    void scansSupportedJsonFilesAndClassifiesTypes() throws Exception {
        Files.createDirectories(archiveDir.resolve("analysis"));
        Files.createDirectories(archiveDir.resolve("odds"));
        Files.createDirectories(archiveDir.resolve("sources"));
        Files.writeString(archiveDir.resolve("bets.json"), "{\"bets\":[{\"bet_id\":\"b1\"}]}");
        Files.writeString(archiveDir.resolve("analysis/match.json"), "{\"id\":\"a1\",\"match\":\"A vs B\"}");
        Files.writeString(archiveDir.resolve("odds/match.json"), "{\"event_id\":\"o1\"}");
        Files.writeString(archiveDir.resolve("sources/source.json"), "{\"id\":\"s1\"}");
        Files.writeString(archiveDir.resolve("analysis/bad.json"), "{bad-json");
        Files.writeString(archiveDir.resolve("analysis/missing-basic-field.json"), "{\"note\":\"missing\"}");
        Files.writeString(archiveDir.resolve("analysis/_模板.json"), "{\"id\":\"template\"}");

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(6);
        assertThat(result.validItems()).isEqualTo(4);
        assertThat(result.invalidItems()).isEqualTo(2);
        assertThat(result.candidates().stream().map(ArchiveScanCandidate::type).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(ImportItemType.BETS, ImportItemType.ANALYSIS, ImportItemType.ODDS, ImportItemType.SOURCE);
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.relativePath()).isEqualTo("analysis/bad.json");
            assertThat(candidate.validJson()).isFalse();
            assertThat(candidate.validationMessage()).contains("JSON");
        });
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.relativePath()).isEqualTo("analysis/missing-basic-field.json");
            assertThat(candidate.validJson()).isFalse();
            assertThat(candidate.validationMessage()).contains("缺少基础字段");
        });
        assertThat(result.candidates()).allSatisfy(candidate -> assertThat(candidate.sha256()).hasSize(64));
    }

    @Test
    void scansFlatInboxJsonByTypeField() throws Exception {
        Files.writeString(archiveDir.resolve("match.json"), """
                {
                  "type": "MATCH",
                  "idempotency_key": "match-20260624-colombia-dr-congo",
                  "payload": {
                    "match_key": "20260624-colombia-dr-congo",
                    "match_name": "哥伦比亚 vs 刚果(金)",
                    "matchday": "2026-06-24"
                  }
                }
                """);

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.validItems()).isEqualTo(1);
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.type()).isEqualTo(ImportItemType.MATCH);
            assertThat(candidate.relativePath()).isEqualTo("match.json");
            assertThat(candidate.summaryTitle()).isEqualTo("哥伦比亚 vs 刚果(金)");
            assertThat(candidate.validationMessage()).isEqualTo("ok");
        });
    }

    @Test
    void flatInboxJsonWithoutTypeIsInvalid() throws Exception {
        Files.writeString(archiveDir.resolve("missing-type.json"), """
                {
                  "payload": {
                    "match_key": "20260624-colombia-dr-congo",
                    "match_name": "哥伦比亚 vs 刚果(金)",
                    "matchday": "2026-06-24"
                  }
                }
                """);

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.invalidItems()).isEqualTo(1);
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.relativePath()).isEqualTo("missing-type.json");
            assertThat(candidate.validJson()).isFalse();
            assertThat(candidate.validationMessage()).contains("type");
        });
    }

    @Test
    void flatInboxJsonWithUnknownTypeIsInvalid() throws Exception {
        Files.writeString(archiveDir.resolve("unknown-type.json"), """
                {
                  "type": "UNKNOWN_KIND",
                  "payload": {"id": "x"}
                }
                """);

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.invalidItems()).isEqualTo(1);
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.relativePath()).isEqualTo("unknown-type.json");
            assertThat(candidate.validJson()).isFalse();
            assertThat(candidate.validationMessage()).contains("不支持").contains("UNKNOWN_KIND");
        });
    }

    @Test
    void scansMasterBusinessAndLegacyArchiveInputsTogether() throws Exception {
        Files.createDirectories(archiveDir.resolve("odds"));
        Files.writeString(archiveDir.resolve("team.json"), """
                {
                  "type": "TEAM",
                  "payload": {
                    "team_key": "france",
                    "display_name": "法国"
                  }
                }
                """);
        Files.writeString(archiveDir.resolve("bet-plan.json"), """
                {
                  "type": "BET_PLAN",
                  "payload": {
                    "plan_key": "plan-france",
                    "title": "法国投注方案"
                  }
                }
                """);
        Files.writeString(archiveDir.resolve("odds/legacy-odds.json"), "{\"event_id\":\"legacy-odds\",\"match\":\"法国 vs 巴西\"}");

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(3);
        assertThat(result.validItems()).isEqualTo(3);
        assertThat(result.candidates().stream().map(ArchiveScanCandidate::type).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(ImportItemType.TEAM, ImportItemType.BET_PLAN, ImportItemType.ODDS);
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.type()).isEqualTo(ImportItemType.TEAM);
            assertThat(candidate.summaryTitle()).isEqualTo("法国");
        });
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.type()).isEqualTo(ImportItemType.BET_PLAN);
            assertThat(candidate.summaryTitle()).isEqualTo("法国投注方案");
        });
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.type()).isEqualTo(ImportItemType.ODDS);
            assertThat(candidate.relativePath()).isEqualTo("odds/legacy-odds.json");
        });
    }
}
