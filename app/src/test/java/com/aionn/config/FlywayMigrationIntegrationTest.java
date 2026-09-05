package com.aionn.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Testcontainers
class FlywayMigrationIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void productionIsCleanAndDemoProfileLoadsFixtures() throws SQLException {
        Flyway production = flyway("classpath:db");
        production.migrate();

        assertThat(rowCount("users")).isZero();
        assertThat(rowCount("orders")).isZero();
        assertThat(rowCount("payments")).isZero();
        assertThat(rowCount("provinces")).isPositive();
        assertThat(rowCount("notification_templates")).isPositive();

        production.clean();
        flyway("classpath:db", "classpath:db-demo").migrate();

        assertThat(rowCount("users")).isPositive();
        assertThat(rowCount("orders")).isPositive();
        assertThat(rowCount("payments")).isPositive();
        assertThat(rowCount("products")).isPositive();
        assertThat(rowCount("promotion_banners")).isZero();
    }

    @Test
    void databaseLockAllowsOnlyOneApplicationInstanceToOwnAJob() {
        Flyway production = flyway("classpath:db");
        production.clean();
        production.migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var firstInstance = new JdbcTemplateLockProvider(dataSource);
        var secondInstance = new JdbcTemplateLockProvider(dataSource);
        var configuration = new LockConfiguration(Instant.now(), "multi-instance-test",
                Duration.ofMinutes(5), Duration.ZERO);

        var firstLock = firstInstance.lock(configuration);
        var competingLock = secondInstance.lock(configuration);

        assertThat(firstLock).isPresent();
        assertThat(competingLock).isEmpty();
        firstLock.orElseThrow().unlock();
        assertThat(secondInstance.lock(configuration)).isPresent();
    }

    @Test
    void durableOperationKeysAreEnforcedByPostgres() throws SQLException {
        Flyway production = flyway("classpath:db");
        production.clean();
        production.migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO order_placement_operations
                        (user_id, idempotency_key, request_hash, order_id)
                    VALUES ('user-1', 'key-1', repeat('a', 64), 'order-1')
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO order_placement_operations
                        (user_id, idempotency_key, request_hash, order_id)
                    VALUES ('user-1', 'key-1', repeat('b', 64), 'order-2')
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void deletedAccountIdentifiersCanBeReusedButActiveDuplicatesRemainRejected() throws SQLException {
        Flyway production = flyway("classpath:db");
        production.clean();
        production.migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users
                        (user_id, email, phone, username, status, deleted_at)
                    VALUES
                        ('deleted-user', 'reuse@example.com', '+84900000001', 'reusable', 'DELETED', NOW()),
                        ('active-user', 'reuse@example.com', '+84900000001', 'reusable', 'ACTIVE', NULL)
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO users
                        (user_id, email, phone, username, status)
                    VALUES
                        ('duplicate-active', 'reuse@example.com', '+84900000001', 'reusable', 'ACTIVE')
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void settlementBucketDeltasReconstructMerchantBalance() throws SQLException {
        Flyway production = flyway("classpath:db");
        production.clean();
        production.migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO merchant_balances
                        (merchant_id, currency, pending, available, receivable, version, created_at, updated_at)
                    VALUES ('merchant-reconcile', 'VND', 10, 20, 5, 0, NOW(), NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO settlement_ledger
                        (entry_id, merchant_id, kind, gross, commission, net,
                         pending_delta, available_delta, receivable_delta, currency)
                    VALUES
                        ('ledger-reconcile', 'merchant-reconcile', 'BALANCE_BASELINE', 0, 0, 0,
                         10, 20, 5, 'VND')
                    """);

            assertThat(settlementMismatchCount(statement)).isZero();
            statement.executeUpdate("""
                    UPDATE merchant_balances
                    SET available = 21
                    WHERE merchant_id = 'merchant-reconcile' AND currency = 'VND'
                    """);
            assertThat(settlementMismatchCount(statement)).isOne();
        }
    }

    @Test
    void settlementMigrationBackfillsHistoricalMovementsBeforeCreatingBaseline() throws SQLException {
        Flyway beforeBucketDeltas = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db")
                .target(MigrationVersion.fromVersion("5.0"))
                .cleanDisabled(false)
                .load();
        beforeBucketDeltas.clean();
        beforeBucketDeltas.migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO merchant_balances
                        (merchant_id, currency, pending, available, version, created_at, updated_at)
                    VALUES ('historical-merchant', 'VND', 0, 95, 0, NOW(), NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO settlement_ledger
                        (entry_id, merchant_id, order_id, kind, gross, commission, net, currency)
                    VALUES
                        ('historical-sale', 'historical-merchant', 'historical-order',
                         'SALE', 100, 5, 95, 'VND'),
                        ('historical-move', 'historical-merchant', 'historical-order',
                         'MOVE_AVAILABLE', 95, 0, 95, 'VND')
                    """);
        }

        flyway("classpath:db").migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet sale = statement.executeQuery("""
                        SELECT pending_delta
                        FROM settlement_ledger
                        WHERE entry_id = 'historical-sale'
                        """)) {
            sale.next();
            assertThat(sale.getBigDecimal(1)).isEqualByComparingTo("95");
            assertThat(settlementMismatchCount(statement)).isZero();
        }
    }

    private static Flyway flyway(String... locations) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(locations)
                .cleanDisabled(false)
                .load();
    }

    private static long rowCount(String table) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static long settlementMismatchCount(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM (
                    SELECT b.merchant_id, b.currency
                    FROM merchant_balances b
                    LEFT JOIN settlement_ledger l
                      ON l.merchant_id = b.merchant_id AND l.currency = b.currency
                    GROUP BY b.merchant_id, b.currency, b.pending, b.available, b.receivable
                    HAVING b.pending <> COALESCE(SUM(l.pending_delta), 0)
                        OR b.available <> COALESCE(SUM(l.available_delta), 0)
                        OR b.receivable <> COALESCE(SUM(l.receivable_delta), 0)
                ) mismatches
                """)) {
            result.next();
            return result.getLong(1);
        }
    }
}
