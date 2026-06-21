package com.worldcup.importreview.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImportJsonCommandLineRunnerTest {

    @Test
    void parsesDryRunCommand() {
        ImportJsonCommandLineRunner.ImportCommand command = ImportJsonCommandLineRunner.parse(
                new String[]{"import-json", "--path", "../skill/archive", "--dry-run"});

        assertThat(command.enabled()).isTrue();
        assertThat(command.archivePath()).isEqualTo("../skill/archive");
        assertThat(command.dryRun()).isTrue();
        assertThat(command.approve()).isFalse();
    }

    @Test
    void parsesApproveCommand() {
        ImportJsonCommandLineRunner.ImportCommand command = ImportJsonCommandLineRunner.parse(
                new String[]{"import-json", "--path", "../skill/archive", "--approve"});

        assertThat(command.enabled()).isTrue();
        assertThat(command.archivePath()).isEqualTo("../skill/archive");
        assertThat(command.dryRun()).isFalse();
        assertThat(command.approve()).isTrue();
    }
}
