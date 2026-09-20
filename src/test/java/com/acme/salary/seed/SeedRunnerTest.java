package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.reference.ReferenceDataService;
import com.acme.salary.support.AbstractIntegrationTest;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeedRunnerTest extends AbstractIntegrationTest {

    private static final int FULL_SEED_COUNT = SeedConstants.DEFAULT_EMPLOYEE_COUNT;

    @Autowired
    private SeedRepository seedRepository;

    @Autowired
    private ReferenceDataService referenceDataService;

    @Autowired
    private Clock clock;

    private SeedRunner runnerFor(int employeeCount) {
        return new SeedRunner(seedRepository, referenceDataService, clock, employeeCount);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    @Test
    void seedsTenThousandEmployeesWithOneInitialHistoryRowEach() {
        runnerFor(FULL_SEED_COUNT).run(null);

        assertThat(count("employee")).isEqualTo(FULL_SEED_COUNT);
        assertThat(count("salary_history")).isEqualTo(FULL_SEED_COUNT);
    }

    @Test
    void doesNothingWhenEmployeesAlreadyExist() {
        runnerFor(50).run(null);
        runnerFor(50).run(null);

        assertThat(count("employee")).isEqualTo(50);
        assertThat(count("salary_history")).isEqualTo(50);
    }

    @Test
    void initialHistoryMatchesEmployeeSalaryAndCountryCurrency() {
        runnerFor(200).run(null);

        Long mismatches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM employee e
                JOIN salary_history h ON h.employee_id = e.id
                JOIN country c ON c.code = e.country_code
                WHERE h.new_salary_minor <> e.salary_minor OR h.currency <> c.currency
                   OR h.old_salary_minor IS NOT NULL OR h.effective_date <> e.hire_date""", Long.class);
        assertThat(mismatches).isZero();
    }
}
