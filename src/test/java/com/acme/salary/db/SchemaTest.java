package com.acme.salary.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class SchemaTest extends AbstractIntegrationTest {

    private static final String INSERT_EMPLOYEE = """
            INSERT INTO employee (employee_code, full_name, email, department_id, job_title_id,
                                  country_code, employment_type, salary_minor, hire_date, status,
                                  created_at, updated_at)
            VALUES (?, 'Test User', ?, 1, 1, ?, 'FULL_TIME', 100000, '2020-01-01', 'ACTIVE',
                    '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z')
            """;

    @Test
    void sqliteConnectionsEnforceForeignKeysAndUseWriteAheadLogging() {
        Integer foreignKeys = jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class);
        String journalMode = jdbcTemplate.queryForObject("PRAGMA journal_mode", String.class);

        assertThat(foreignKeys).isEqualTo(1);
        assertThat(journalMode).isEqualToIgnoringCase("wal");
    }

    @Test
    void referenceDataIsLoadedByMigrations() {
        assertThat(count("country")).isEqualTo(10);
        assertThat(count("currency")).isEqualTo(9);
        assertThat(count("department")).isEqualTo(8);
        assertThat(count("job_title")).isEqualTo(20);
    }

    @Test
    void everyCountryReferencesAnExistingCurrency() {
        Integer orphans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM country c LEFT JOIN currency cu ON cu.code = c.currency WHERE cu.code IS NULL",
                Integer.class);

        assertThat(orphans).isZero();
    }

    @Test
    void rejectsEmployeeWithUnknownCountry() {
        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_EMPLOYEE, "EMP-1", "a@acme.test", "XX"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        jdbcTemplate.update(INSERT_EMPLOYEE, "EMP-1", "same@acme.test", "US");

        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_EMPLOYEE, "EMP-2", "same@acme.test", "US"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int count(String table) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return total == null ? 0 : total;
    }
}
