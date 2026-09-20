package com.acme.salary.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for tests that need the full application context and a real SQLite file.
 * All subclasses share one temporary database; transactional tables are emptied before each test.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final Path DATABASE_FILE = createTemporaryDatabaseFile();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_FILE);
        registry.add("app.seed.enabled", () -> "false");
    }

    @BeforeEach
    void clearTransactionalTables() {
        jdbcTemplate.execute("DELETE FROM salary_history");
        jdbcTemplate.execute("DELETE FROM employee");
    }

    private static Path createTemporaryDatabaseFile() {
        try {
            Path file = Files.createTempFile("salary-test-", ".db");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create temporary test database", e);
        }
    }
}
