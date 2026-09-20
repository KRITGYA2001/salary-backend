package com.acme.salary.salary;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SalaryHistoryRepository {

    private static final String INSERT_HISTORY = """
            INSERT INTO salary_history (employee_id, old_salary_minor, new_salary_minor, currency,
                                        effective_date, reason, changed_at)
            VALUES (:employeeId, :oldSalaryMinor, :newSalaryMinor, :currency, :effectiveDate, :reason, :changedAt)""";

    private final JdbcClient jdbcClient;

    public SalaryHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Appends a history entry; {@code oldSalaryMinor} is null for an employee's first salary. */
    public void insert(long employeeId, Long oldSalaryMinor, long newSalaryMinor, String currency,
                       LocalDate effectiveDate, String reason, Instant changedAt) {
        jdbcClient.sql(INSERT_HISTORY)
                .param("employeeId", employeeId)
                .param("oldSalaryMinor", oldSalaryMinor)
                .param("newSalaryMinor", newSalaryMinor)
                .param("currency", currency)
                .param("effectiveDate", effectiveDate.toString())
                .param("reason", reason)
                .param("changedAt", changedAt.toString())
                .update();
    }
}
