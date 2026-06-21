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
        Files.writeString(archiveDir.resolve("analysis/_模板.json"), "{\"id\":\"template\"}");

        var scanner = new JsonArchiveScanner(new ObjectMapper());

        ArchiveScanResult result = scanner.scan(archiveDir);

        assertThat(result.totalItems()).isEqualTo(5);
        assertThat(result.validItems()).isEqualTo(4);
        assertThat(result.invalidItems()).isEqualTo(1);
        assertThat(result.candidates().stream().map(ArchiveScanCandidate::type).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(ImportItemType.BETS, ImportItemType.ANALYSIS, ImportItemType.ODDS, ImportItemType.SOURCE);
        assertThat(result.candidates()).anySatisfy(candidate -> {
            assertThat(candidate.relativePath()).isEqualTo("analysis/bad.json");
            assertThat(candidate.validJson()).isFalse();
            assertThat(candidate.validationMessage()).contains("JSON");
        });
        assertThat(result.candidates()).allSatisfy(candidate -> assertThat(candidate.sha256()).hasSize(64));
    }
}
