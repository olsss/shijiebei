package com.worldcup.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationCompatibilityTest {
    private static final int MYSQL_UTF8MB4_MAX_INDEX_BYTES = 3072;
    private static final int UTF8MB4_MAX_BYTES_PER_CHAR = 4;
    private static final int BIGINT_INDEX_BYTES = 8;

    @Test
    void importItemRelativePathUniqueKeyFitsMysqlUtf8mb4IndexLimit() throws Exception {
        String sql;
        try (var stream = getClass().getClassLoader().getResourceAsStream("db/migration/V2__create_import_review_tables.sql")) {
            assertThat(stream).as("V2 migration resource").isNotNull();
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        var matcher = Pattern.compile("relative_path\\s+VARCHAR\\((\\d+)\\)", Pattern.CASE_INSENSITIVE)
                .matcher(sql);

        assertThat(matcher.find()).as("relative_path VARCHAR length is declared").isTrue();
        int relativePathLength = Integer.parseInt(matcher.group(1));

        int uniqueIndexBytes = BIGINT_INDEX_BYTES + relativePathLength * UTF8MB4_MAX_BYTES_PER_CHAR;
        assertThat(uniqueIndexBytes)
                .as("uk_import_items_job_path must fit MySQL utf8mb4 3072-byte key limit")
                .isLessThanOrEqualTo(MYSQL_UTF8MB4_MAX_INDEX_BYTES);
    }
}
