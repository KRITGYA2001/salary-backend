package com.acme.salary.employee;

import static com.acme.salary.employee.EmployeeSql.FROM_EMPLOYEE_JOINS;
import static com.acme.salary.employee.EmployeeSql.SALARY_USD;

import com.acme.salary.util.MoneyUtils;
import com.acme.salary.util.SqlUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepository {

    private static final int USD_SCALE = 2;

    private static final String SELECT_SUMMARY = """
            SELECT e.id, e.employee_code, e.full_name, e.email, d.name AS department, jt.title AS job_title,
                   e.country_code, co.name AS country_name, co.currency, e.employment_type, e.salary_minor,
                   cur.minor_unit_exponent, %s AS salary_usd, e.hire_date, e.status
            """.formatted(SALARY_USD);

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
            conditions.add("(e.full_name LIKE :search ESCAPE '%1$s' OR e.employee_code LIKE :search ESCAPE '%1$s')"
                    .formatted(SqlUtils.LIKE_ESCAPE));
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
                rs.getString("department"),
                rs.getString("job_title"),
                rs.getString("country_code"),
                rs.getString("country_name"),
                rs.getString("currency"),
                EmploymentType.valueOf(rs.getString("employment_type")),
                MoneyUtils.toMajorUnits(rs.getLong("salary_minor"), exponent),
                BigDecimal.valueOf(rs.getDouble("salary_usd")).setScale(USD_SCALE, RoundingMode.HALF_UP),
                LocalDate.parse(rs.getString("hire_date")),
                EmployeeStatus.valueOf(rs.getString("status")));
    }
}
