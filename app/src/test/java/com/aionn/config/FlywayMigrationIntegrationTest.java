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
}
