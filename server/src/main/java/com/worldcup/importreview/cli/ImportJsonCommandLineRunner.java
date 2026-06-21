package com.worldcup.importreview.cli;

import com.worldcup.importreview.service.JsonImportReviewService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;

@Component
public class ImportJsonCommandLineRunner implements CommandLineRunner {
    private final JsonImportReviewService service;

    public ImportJsonCommandLineRunner(JsonImportReviewService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        ImportCommand command = parse(args);
        if (!command.enabled()) {
            return;
        }
        Path archive = Path.of(command.archivePath());
        if (command.dryRun()) {
            var result = service.dryRun(archive);
            System.out.printf("JSON dry-run: total=%d valid=%d invalid=%d path=%s%n",
                    result.totalItems(), result.validItems(), result.invalidItems(), result.archivePath());
            return;
        }
        if (command.approve()) {
            var job = service.approveRun(archive, "cli");
            System.out.printf("JSON approve import: job=%d total=%d valid=%d invalid=%d path=%s%n",
                    job.id(), job.totalItems(), job.validItems(), job.invalidItems(), job.archivePath());
            return;
        }
        throw new IllegalArgumentException("import-json 需要 --dry-run 或 --approve");
    }

    public static ImportCommand parse(String[] args) {
        if (args == null || Arrays.stream(args).noneMatch("import-json"::equals)) {
            return new ImportCommand(false, "", false, false);
        }
        String path = "../skill/archive";
        boolean dryRun = false;
        boolean approve = false;
        for (int i = 0; i < args.length; i++) {
            if ("--path".equals(args[i]) && i + 1 < args.length) {
                path = args[++i];
            } else if ("--dry-run".equals(args[i])) {
                dryRun = true;
            } else if ("--approve".equals(args[i])) {
                approve = true;
            }
        }
        return new ImportCommand(true, path, dryRun, approve);
    }

    public record ImportCommand(boolean enabled, String archivePath, boolean dryRun, boolean approve) {
    }
}
