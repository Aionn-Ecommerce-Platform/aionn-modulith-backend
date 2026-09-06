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

    @FunctionalInterface
    private interface StatementPredicate {
        boolean matches(String statement);
    }

    private record ApprovedBackfill(String table, StatementPredicate predicate) {
    }

    private static final Map<String, List<ApprovedBackfill>> APPROVED_SCHEMA_BACKFILLS = Map.of(
            "modules/identity/src/main/resources/db/V1.2__complete_account_deletion.sql", List.of(
                    new ApprovedBackfill("users",
                            stmt -> stmt.equals("update users set email = null, phone = null, username = 'deleted_' || user_id, password_hash = null, display_name = 'deleted user', avatar_url = null, email_verified_at = null, phone_verified_at = null, mfa_enabled = false, mfa_secret = null, failed_login_attempts = 0, locked_until = null, deleted_at = coalesce(deleted_at, now()) where status = 'deleted'"))),
            "modules/promotion/src/main/resources/db/V8.5__harden_banner_assets_and_ordering.sql", List.of(
                    new ApprovedBackfill("promotion_banners",
                            stmt -> stmt.equals("update promotion_banners set image_public_id = 'legacy/promotion/banners/' || banner_id where image_public_id is null or btrim(image_public_id) = ''"))));

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
            "chat_messages",
            "recommendation_interactions");

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
            String rawSql = Files.readString(path);
            String sql = stripSqlComments(rawSql);
            var matcher = DML_TARGET.matcher(sql);
            while (matcher.find()) {
                String table = matcher.group(1).toLowerCase(Locale.ROOT);
                if (DEMO_TABLES.contains(table)) {
                    String statement = extractStatementAt(sql, matcher.start());
                    if (!isApprovedSchemaBackfill(path, table, statement)) {
                        violations.add(path + " writes demo table " + table + " via statement: " + statement);
                    }
                }
            }
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

    private static String stripSqlComments(String sql) {
        return sql.replaceAll("--[^\\r\\n]*", "").replaceAll("/\\*[\\s\\S]*?\\*/", "");
    }

    private static String extractStatementAt(String sql, int matchStart) {
        int semicolonIndex = sql.indexOf(';', matchStart);
        String raw = (semicolonIndex >= 0) ? sql.substring(matchStart, semicolonIndex) : sql.substring(matchStart);
        return raw.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isApprovedSchemaBackfill(Path path, String table, String statement) {
        String normalizedPath = normalized(path);
        return APPROVED_SCHEMA_BACKFILLS.entrySet().stream()
                .filter(entry -> normalizedPath.endsWith(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(backfill -> backfill.table().equals(table) && backfill.predicate().matches(statement));
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
