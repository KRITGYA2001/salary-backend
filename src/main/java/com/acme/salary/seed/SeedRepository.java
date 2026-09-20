package com.acme.salary.seed;

import static com.acme.salary.seed.SeedConstants.INITIAL_SALARY_REASON;
import static com.acme.salary.seed.SeedConstants.INSERT_BATCH_SIZE;

import com.acme.salary.employee.NewEmployee;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SeedRepository {

    private static final String INSERT_EMPLOYEE = """
            INSERT INTO employee (employee_code, full_name, email, department_id, job_title_id, country_code,
                                  employment_type, salary_minor, hire_date, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String INSERT_INITIAL_HISTORY = """
            INSERT INTO salary_history (employee_id, old_salary_minor, new_salary_minor, currency,
                                        effective_date, reason, changed_at)
            SELECT e.id, NULL, e.salary_minor, c.currency, e.hire_date, ?, ?
            FROM employee e JOIN country c ON c.code = e.country_code""";

    private final JdbcTemplate jdbcTemplate;

    public SeedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasEmployees() {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM employee)", Boolean.class));
    }

    /** Inserts all employees plus one initial salary-history row each, atomically, using JDBC batches. */
    @Transactional
    public void insertAll(List<NewEmployee> employees, Instant now) {
        String timestamp = now.toString();
        jdbcTemplate.batchUpdate(INSERT_EMPLOYEE, employees, INSERT_BATCH_SIZE, (statement, employee) -> {
            statement.setString(1, employee.employeeCode());
            statement.setString(2, employee.fullName());
            statement.setString(3, employee.email());
            statement.setLong(4, employee.departmentId());
            statement.setLong(5, employee.jobTitleId());
            statement.setString(6, employee.countryCode());
            statement.setString(7, employee.employmentType().name());
            statement.setLong(8, employee.salaryMinor());
            statement.setString(9, employee.hireDate().toString());
            statement.setString(10, employee.status().name());
            statement.setString(11, timestamp);
            statement.setString(12, timestamp);
        });
        jdbcTemplate.update(INSERT_INITIAL_HISTORY, INITIAL_SALARY_REASON, timestamp);
    }
}
