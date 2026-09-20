package com.acme.salary.insights;

import static com.acme.salary.employee.EmployeeSql.FROM_EMPLOYEE_JOINS;
import static com.acme.salary.employee.EmployeeSql.SALARY_USD;
import static com.acme.salary.insights.InsightsConstants.HIGH_PERCENTILE;
import static com.acme.salary.insights.InsightsConstants.MEDIAN_PERCENTILE;

import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.util.MoneyUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Aggregate queries over active employees. Percentiles are computed with window functions using the
 * nearest-rank rule: the row at position {@code ceil(p * n / 100)} of the ascending salaries.
 */
@Repository
public class InsightsRepository {

    private static final String ALL_KEY = "ALL";
    private static final String ALL_LABEL = "All employees";

    private static final String STATS_QUERY = """
            WITH salaries AS (
                SELECT %1$s AS group_key, %2$s AS group_label, %3$s AS usd
                %4$s
            ),
            ranked AS (
                SELECT group_key, group_label, usd,
                       ROW_NUMBER() OVER (PARTITION BY group_key ORDER BY usd) AS position,
                       COUNT(*) OVER (PARTITION BY group_key) AS group_size
                FROM salaries
            )
            SELECT group_key, group_label, MAX(group_size) AS headcount, AVG(usd) AS average_usd,
                   MIN(usd) AS min_usd, MAX(usd) AS max_usd,
                   MAX(CASE WHEN position = (group_size * %5$d + 99) / 100 THEN usd END) AS median_usd,
                   MAX(CASE WHEN position = (group_size * %6$d + 99) / 100 THEN usd END) AS p90_usd
            FROM ranked
            GROUP BY group_key, group_label
            ORDER BY group_label""";

    private final JdbcClient jdbcClient;

    public InsightsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SalaryStats summarize(InsightsFilter filter) {
        return queryStats("'" + ALL_KEY + "'", "'" + ALL_LABEL + "'", filter).stream()
                .findFirst()
                .map(GroupSalaryStats::stats)
                .orElse(SalaryStats.EMPTY);
    }

    public List<GroupSalaryStats> groupBy(InsightDimension dimension, InsightsFilter filter) {
        return queryStats(dimension.keySql(), dimension.labelSql(), filter);
    }

    /** Returns headcount plus lowest and highest salary, used to size histogram buckets. */
    public SalaryRange findRange(InsightsFilter filter) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String from = fromActiveEmployees(filter, parameters);
        JdbcClient.StatementSpec statement = jdbcClient.sql(
                "SELECT COUNT(*) AS headcount, MIN(%1$s) AS min_usd, MAX(%1$s) AS max_usd %2$s"
                        .formatted(SALARY_USD, from));
        parameters.forEach(statement::param);
        return statement.query((rs, rowNumber) -> new SalaryRange(
                rs.getLong("headcount"), rs.getDouble("min_usd"), rs.getDouble("max_usd"))).single();
    }

    /**
     * Counts employees per equal-width bucket between {@code minUsd} and {@code maxUsd}; buckets with no
     * employees are absent from the result. The highest salary falls into the last bucket.
     */
    public Map<Integer, Long> countByBucket(InsightsFilter filter, double minUsd, double maxUsd, int bucketCount) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String from = fromActiveEmployees(filter, parameters);
        JdbcClient.StatementSpec statement = jdbcClient.sql("""
                SELECT MIN(CAST((%1$s - :minUsd) * :bucketCount / (:maxUsd - :minUsd) AS INTEGER),
                           :bucketCount - 1) AS bucket_index,
                       COUNT(*) AS bucket_size
                %2$s
                GROUP BY bucket_index""".formatted(SALARY_USD, from))
                .param("minUsd", minUsd)
                .param("maxUsd", maxUsd)
                .param("bucketCount", bucketCount);
        parameters.forEach(statement::param);

        Map<Integer, Long> countsByIndex = new LinkedHashMap<>();
        statement.query((rs, rowNumber) -> countsByIndex.put(rs.getInt("bucket_index"), rs.getLong("bucket_size")))
                .list();
        return countsByIndex;
    }

    private List<GroupSalaryStats> queryStats(String keySql, String labelSql, InsightsFilter filter) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String from = fromActiveEmployees(filter, parameters);
        JdbcClient.StatementSpec statement = jdbcClient.sql(STATS_QUERY.formatted(
                keySql, labelSql, SALARY_USD, from, MEDIAN_PERCENTILE, HIGH_PERCENTILE));
        parameters.forEach(statement::param);
        return statement.query(InsightsRepository::mapGroupStats).list();
    }

    private static String fromActiveEmployees(InsightsFilter filter, Map<String, Object> parameters) {
        List<String> conditions = new ArrayList<>();
        conditions.add("e.status = :activeStatus");
        parameters.put("activeStatus", EmployeeStatus.ACTIVE.name());
        if (filter.countryCode() != null) {
            conditions.add("e.country_code = :countryCode");
            parameters.put("countryCode", filter.countryCode());
        }
        if (filter.departmentId() != null) {
            conditions.add("e.department_id = :departmentId");
            parameters.put("departmentId", filter.departmentId());
        }
        if (filter.jobTitleId() != null) {
            conditions.add("e.job_title_id = :jobTitleId");
            parameters.put("jobTitleId", filter.jobTitleId());
        }
        return FROM_EMPLOYEE_JOINS + " WHERE " + String.join(" AND ", conditions);
    }

    private static GroupSalaryStats mapGroupStats(ResultSet rs, int rowNumber) throws SQLException {
        SalaryStats stats = new SalaryStats(
                rs.getLong("headcount"),
                MoneyUtils.roundUsd(rs.getDouble("average_usd")),
                MoneyUtils.roundUsd(rs.getDouble("median_usd")),
                MoneyUtils.roundUsd(rs.getDouble("p90_usd")),
                MoneyUtils.roundUsd(rs.getDouble("min_usd")),
                MoneyUtils.roundUsd(rs.getDouble("max_usd")));
        return new GroupSalaryStats(rs.getString("group_key"), rs.getString("group_label"), stats);
    }

    /** Headcount with the lowest and highest salary in US dollars. */
    public record SalaryRange(long headcount, double minUsd, double maxUsd) {
    }
}
