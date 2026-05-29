package com.parkdh.stockadvisor.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationNamingTest {
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @Test
    void migrationVersionsAreUniqueAndContiguous() throws Exception {
        Path migrationDir = Path.of("src/main/resources/db/migration");

        List<Integer> versions = Files.list(migrationDir)
                .map(path -> path.getFileName().toString())
                .map(MIGRATION_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> Integer.parseInt(matcher.group(1)))
                .sorted()
                .toList();

        assertThat(versions).isNotEmpty();
        assertThat(versions).doesNotHaveDuplicates();
        assertThat(versions).containsExactlyElementsOf(java.util.stream.IntStream
                .rangeClosed(1, versions.getLast())
                .boxed()
                .toList());
    }
}
