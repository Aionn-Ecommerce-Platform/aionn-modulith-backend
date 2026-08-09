package com.aionn.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductionMigrationSafetyTest {

    private static final Map<String, Set<String>> APPROVED_SCHEMA_BACKFILLS = Map.of(
            "modules/identity/src/main/resources/db/V1.2__complete_account_deletion.sql", Set.of("users"));

    private static final Set<String> DEMO_TABLES = Set.of(
            "users",
            "user_roles",
            "merchants",
            "categories",
            "brands",
            "products",
            "product_variants",
            "product_reviews",
            "category_translations",
            "product_sold_counters",
            "product_translations",
            "warehouses",
            "inventory_items",
            "orders",
            "order_items",
            "payments",
            "promotion_campaigns",
            "vouchers",
            "promotion_banners",
            "flash_sale_registrations",
            "notifications",
            "chat_conversations",
            "chat_messages");

    private static final Pattern DML_TARGET = Pattern.compile(
            "(?i)\\b(?:insert\\s+into|update|delete\\s+from|truncate(?:\\s+table)?)\\s+"
                    + "(?:[a-z0-9_]+\\.)?\\\"?([a-z0-9_]+)\\\"?");
    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$\\d{2}\\$");
    private static final Pattern SAMPLE_ORDER = Pattern.compile("(?i)\\bORD_[0-9]{3,}\\b");
    private static final Pattern PROCEDURAL_BLOCK = Pattern.compile("(?i)\\bDO\\s+\\$\\$");

    @Test
    void productionMigrationsContainOnlySchemaAndApprovedReferenceData() throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repositoryRoot.resolve("modules"))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("V.*\\.sql"))
                    .filter(path -> normalized(path).contains("/src/main/resources/db/"))
                    .filter(path -> !normalized(path).contains("/db-demo/"))
                    .filter(path -> !normalized(path).contains("/bin/"))
                    .filter(path -> !normalized(path).contains("/build/"))
                    .forEach(path -> inspect(path, violations));
        }

        assertThat(violations)
                .as("Production Flyway migrations must not contain demo fixtures")
                .isEmpty();
    }

    private static String normalized(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    private static void inspect(Path path, List<String> violations) {
        try {
            String sql = Files.readString(path);
            DML_TARGET.matcher(sql).results()
                    .map(result -> result.group(1).toLowerCase())
                    .filter(DEMO_TABLES::contains)
                    .filter(table -> !isApprovedSchemaBackfill(path, table))
                    .forEach(table -> violations.add(path + " writes demo table " + table));
            if (sql.toLowerCase(Locale.ROOT).contains("@aionn.com")) {
                violations.add(path + " contains an @aionn.com demo email");
            }
            if (BCRYPT_HASH.matcher(sql).find()) {
                violations.add(path + " contains a BCrypt password hash");
            }
            if (SAMPLE_ORDER.matcher(sql).find()) {
                violations.add(path + " contains a sample order identifier");
            }
            if (PROCEDURAL_BLOCK.matcher(sql).find()) {
                violations.add(path + " contains a procedural block; review it outside production migrations");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect migration " + path, exception);
        }
    }

    private static boolean isApprovedSchemaBackfill(Path path, String table) {
        String normalizedPath = normalized(path);
        return APPROVED_SCHEMA_BACKFILLS.entrySet().stream()
                .anyMatch(entry -> normalizedPath.endsWith(entry.getKey()) && entry.getValue().contains(table));
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Unable to locate repository root");
        }
        return current;
    }
}
