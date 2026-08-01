package com.aionn.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
