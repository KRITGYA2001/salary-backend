package com.acme.salary.employee;

import static com.acme.salary.employee.EmployeeConstraints.EMPLOYEE_CODE_FORMAT;
import static com.acme.salary.employee.EmployeeSql.FROM_EMPLOYEE_JOINS;
import static com.acme.salary.employee.EmployeeSql.SALARY_USD;

import com.acme.salary.util.MoneyUtils;
import com.acme.salary.util.SqlUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepository {

    private static final String PENDING_CODE_PREFIX = "PENDING-";

    private static final String SELECT_SUMMARY = """
            SELECT e.id, e.employee_code, e.full_name, e.email, e.department_id, d.name AS department,
                   e.job_title_id, jt.title AS job_title, e.country_code, co.name AS country_name, co.currency,
                   e.employment_type, e.salary_minor, cur.minor_unit_exponent, %s AS salary_usd,
                   e.hire_date, e.status
            """.formatted(SALARY_USD);

    private static final String SEARCH_CONDITION = """
            (e.full_name LIKE :search ESCAPE '%1$s' OR e.employee_code LIKE :search ESCAPE '%1$s'
             OR e.email LIKE :search ESCAPE '%1$s')""".formatted(SqlUtils.LIKE_ESCAPE);

    private static final String INSERT_EMPLOYEE = """
            INSERT INTO employee (employee_code, full_name, email, department_id, job_title_id, country_code,
                                  employment_type, salary_minor, hire_date, status, created_at, updated_at)
            VALUES (:code, :fullName, :email, :departmentId, :jobTitleId, :countryCode,
                    :employmentType, :salaryMinor, :hireDate, :status, :now, :now)""";

    private static final String UPDATE_PROFILE = """
            UPDATE employee
            SET full_name = :fullName, email = :email, department_id = :departmentId,
                job_title_id = :jobTitleId, employment_type = :employmentType, updated_at = :now
            WHERE id = :id""";

    private static final String SELECT_SALARY_CONTEXT = """
            SELECT e.salary_minor, co.currency, cur.minor_unit_exponent, e.status, e.hire_date
            FROM employee e
            JOIN country co ON co.code = e.country_code
            JOIN currency cur ON cur.code = co.currency
            WHERE e.id = :id""";

    private final JdbcClient jdbcClient;

    public EmployeeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<EmployeeSummary> search(EmployeeSearchCriteria criteria) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = buildWhereClause(criteria, parameters);
        String sql = SELECT_SUMMARY + FROM_EMPLOYEE_JOINS + where
                + " ORDER BY " + criteria.sortField().sqlExpression() + " " + criteria.sortDirection().name()
                + ", e.id LIMIT :limit OFFSET :offset";

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql)
                .param("limit", criteria.size())
                .param("offset", criteria.offset());
        parameters.forEach(statement::param);
        return statement.query(EmployeeRepository::mapSummary).list();
    }

    /**
     * Streams every employee matching the criteria in sort order, ignoring paging, so callers can export
     * large result sets without holding them in memory.
     */
    public void forEachMatching(EmployeeSearchCriteria criteria, Consumer<EmployeeSummary> consumer) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = buildWhereClause(criteria, parameters);
        String sql = SELECT_SUMMARY + FROM_EMPLOYEE_JOINS + where
                + " ORDER BY " + criteria.sortField().sqlExpression() + " " + criteria.sortDirection().name()
                + ", e.id";

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql);
        parameters.forEach(statement::param);
        try (Stream<EmployeeSummary> rows = statement.query(EmployeeRepository::mapSummary).stream()) {
            rows.forEach(consumer);
        }
    }

    public long count(EmployeeSearchCriteria criteria) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = buildWhereClause(criteria, parameters);

        JdbcClient.StatementSpec statement = jdbcClient.sql("SELECT COUNT(*) " + FROM_EMPLOYEE_JOINS + where);
        parameters.forEach(statement::param);
        return statement.query(Long.class).single();
    }

    private static String buildWhereClause(EmployeeSearchCriteria criteria, Map<String, Object> parameters) {
        List<String> conditions = new ArrayList<>();
        if (criteria.countryCode() != null) {
            conditions.add("e.country_code = :countryCode");
            parameters.put("countryCode", criteria.countryCode());
        }
        if (criteria.departmentId() != null) {
            conditions.add("e.department_id = :departmentId");
            parameters.put("departmentId", criteria.departmentId());
        }
        if (criteria.jobTitleId() != null) {
            conditions.add("e.job_title_id = :jobTitleId");
            parameters.put("jobTitleId", criteria.jobTitleId());
        }
        if (criteria.status() != null) {
            conditions.add("e.status = :status");
            parameters.put("status", criteria.status().name());
        }
        if (criteria.search() != null) {
            conditions.add(SEARCH_CONDITION);
            parameters.put("search", SqlUtils.prefixPattern(criteria.search()));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static EmployeeSummary mapSummary(ResultSet rs, int rowNumber) throws SQLException {
        int exponent = rs.getInt("minor_unit_exponent");
        return new EmployeeSummary(
                rs.getLong("id"),
                rs.getString("employee_code"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getLong("department_id"),
                rs.getString("department"),
                rs.getLong("job_title_id"),
                rs.getString("job_title"),
                rs.getString("country_code"),
                rs.getString("country_name"),
                rs.getString("currency"),
                EmploymentType.valueOf(rs.getString("employment_type")),
                MoneyUtils.toMajorUnits(rs.getLong("salary_minor"), exponent),
                MoneyUtils.roundUsd(rs.getDouble("salary_usd")),
                LocalDate.parse(rs.getString("hire_date")),
                EmployeeStatus.valueOf(rs.getString("status")));
    }

    public Optional<EmployeeSummary> findById(long id) {
        return jdbcClient.sql(SELECT_SUMMARY + FROM_EMPLOYEE_JOINS + " WHERE e.id = :id")
                .param("id", id)
                .query(EmployeeRepository::mapSummary)
                .optional();
    }

    /** Inserts the employee and returns the generated id; the final employee code is derived from it. */
    public long insert(NewEmployee employee, Instant now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(INSERT_EMPLOYEE)
                .param("code", PENDING_CODE_PREFIX + UUID.randomUUID())
                .param("fullName", employee.fullName())
                .param("email", employee.email())
                .param("departmentId", employee.departmentId())
                .param("jobTitleId", employee.jobTitleId())
                .param("countryCode", employee.countryCode())
                .param("employmentType", employee.employmentType().name())
                .param("salaryMinor", employee.salaryMinor())
                .param("hireDate", employee.hireDate().toString())
                .param("status", employee.status().name())
                .param("now", now.toString())
                .update(keyHolder);
        long id = keyHolder.getKey().longValue();
        jdbcClient.sql("UPDATE employee SET employee_code = :code WHERE id = :id")
                .param("code", EMPLOYEE_CODE_FORMAT.formatted(id))
                .param("id", id)
                .update();
        return id;
    }

    public void updateProfile(long id, String fullName, String email, long departmentId, long jobTitleId,
                              EmploymentType employmentType, Instant now) {
        jdbcClient.sql(UPDATE_PROFILE)
                .param("id", id)
                .param("fullName", fullName)
                .param("email", email)
                .param("departmentId", departmentId)
                .param("jobTitleId", jobTitleId)
                .param("employmentType", employmentType.name())
                .param("now", now.toString())
                .update();
    }

    public void markInactive(long id, Instant now) {
        jdbcClient.sql("UPDATE employee SET status = :status, updated_at = :now WHERE id = :id")
                .param("status", EmployeeStatus.INACTIVE.name())
                .param("now", now.toString())
                .param("id", id)
                .update();
    }

    public Optional<SalaryContext> findSalaryContext(long id) {
        return jdbcClient.sql(SELECT_SALARY_CONTEXT)
                .param("id", id)
                .query((rs, rowNumber) -> new SalaryContext(
                        rs.getLong("salary_minor"),
                        rs.getString("currency"),
                        rs.getInt("minor_unit_exponent"),
                        EmployeeStatus.valueOf(rs.getString("status")),
                        LocalDate.parse(rs.getString("hire_date"))))
                .optional();
    }

    /**
     * Sets the new salary only if it still equals the value the caller read, so concurrent changes cannot
     * silently overwrite each other.
     *
     * @return {@code false} if the salary was changed in the meantime
     */
    public boolean updateSalary(long id, long expectedSalaryMinor, long newSalaryMinor, Instant now) {
        int updatedRows = jdbcClient.sql("""
                        UPDATE employee SET salary_minor = :newSalary, updated_at = :now
                        WHERE id = :id AND salary_minor = :expectedSalary""")
                .param("newSalary", newSalaryMinor)
                .param("now", now.toString())
                .param("id", id)
                .param("expectedSalary", expectedSalaryMinor)
                .update();
        return updatedRows == 1;
    }
}
