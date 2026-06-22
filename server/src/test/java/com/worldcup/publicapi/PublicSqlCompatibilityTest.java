package com.worldcup.publicapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicSqlCompatibilityTest {
    @Test
    void publicPrematchSqlDoesNotUseH2OnlyDateadd() throws IOException {
        String source = Files.readString(publicPrematchServiceSource());

        assertFalse(source.toUpperCase().contains("DATEADD"),
                "Public prematch SQL must not use H2-only DATEADD; bind Java-computed cutoffs instead.");
    }

    private Path publicPrematchServiceSource() {
        Path fromServerBase = Path.of("src/main/java/com/worldcup/publicapi/service/PublicPrematchWorkbenchService.java");
        if (Files.exists(fromServerBase)) {
            return fromServerBase;
        }
        return Path.of("server/src/main/java/com/worldcup/publicapi/service/PublicPrematchWorkbenchService.java");
    }
}
