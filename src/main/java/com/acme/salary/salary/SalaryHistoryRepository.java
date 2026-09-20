package com.acme.salary.salary;

import com.acme.salary.util.MoneyUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SalaryHistoryRepository {

    private static final String INSERT_HISTORY = """
            INSERT INTO salary_history (employee_id, old_salary_minor, new_salary_minor, currency,
                                        effective_date, reason, changed_at)
            VALUES (:employeeId, :oldSalaryMinor, :newSalaryMinor, :currency, :effectiveDate, :reason, :changedAt)""";

    private static final String SELECT_HISTORY = """
            SELECT h.id, h.old_salary_minor, h.new_salary_minor, h.currency, cur.minor_unit_exponent,
                   h.effective_date, h.reason, h.changed_at
            FROM salary_history h
            JOIN currency cur ON cur.code = h.currency
            WHERE h.employee_id = :employeeId
            ORDER BY h.changed_at DESC, h.id DESC""";

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

    /** Returns the employee's salary changes, newest first. */
    public List<SalaryHistoryEntry> findByEmployeeId(long employeeId) {
        return jdbcClient.sql(SELECT_HISTORY)
                .param("employeeId", employeeId)
                .query(SalaryHistoryRepository::mapEntry)
                .list();
    }

    private static SalaryHistoryEntry mapEntry(ResultSet rs, int rowNumber) throws SQLException {
        int exponent = rs.getInt("minor_unit_exponent");
        long oldSalaryMinor = rs.getLong("old_salary_minor");
        BigDecimal oldSalary = rs.wasNull() ? null : MoneyUtils.toMajorUnits(oldSalaryMinor, exponent);
        return new SalaryHistoryEntry(
                rs.getLong("id"),
                oldSalary,
                MoneyUtils.toMajorUnits(rs.getLong("new_salary_minor"), exponent),
                rs.getString("currency"),
                LocalDate.parse(rs.getString("effective_date")),
                rs.getString("reason"),
                Instant.parse(rs.getString("changed_at")));
    }
}
