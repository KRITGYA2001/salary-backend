package com.acme.salary.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.employee.EmployeeSql;
import com.acme.salary.reference.ReferenceDataService;
import com.acme.salary.seed.SeedConstants;
import com.acme.salary.seed.SeedRepository;
import com.acme.salary.seed.SeedRunner;
import com.acme.salary.support.AbstractIntegrationTest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Seeds the full 10,000 employees and checks the budgets in docs/performance.md.
 * Excluded from the default build; run with {@code ./mvnw test -Pperf -Dtest=PerformanceBudgetTest}.
 */
@Tag("perf")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceBudgetTest extends AbstractIntegrationTest {

    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 30;
    private static final double LIST_BUDGET_MS = 300;
    private static final double INSIGHT_BUDGET_MS = 500;
    private static final long SEED_BUDGET_MS = 5_000;
    private static final String API = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    @Autowired
    private ReferenceDataService referenceDataService;

    @Autowired
    private Clock clock;

    private long seedMillis;

    @BeforeAll
    void seedFullDataset() {
        jdbcTemplate.execute("DELETE FROM salary_history");
        jdbcTemplate.execute("DELETE FROM employee");
        long started = System.nanoTime();
        new SeedRunner(seedRepository, referenceDataService, clock, SeedConstants.DEFAULT_EMPLOYEE_COUNT).run(null);
        seedMillis = (System.nanoTime() - started) / 1_000_000;
        jdbcTemplate.execute("ANALYZE");
    }

    @Test
    void seedStaysWithinBudget() {
        report("seed 10k", seedMillis);
        assertThat(seedMillis).isLessThan(SEED_BUDGET_MS);
    }

    @Test
    void indexedListFiltersDoNotScanTheEmployeeTable() {
        List<String> filtered = queryPlan("WHERE e.country_code = 'IN' AND e.department_id = 2");
        List<String> byStatus = queryPlan("WHERE e.status = 'ACTIVE' AND e.country_code = 'US'");
        List<String> byName = queryPlan("WHERE e.full_name LIKE 'Asha%' ORDER BY e.full_name COLLATE NOCASE");

        filtered.forEach(step -> report("plan country+dept", step));
        assertThat(filtered).noneMatch(PerformanceBudgetTest::scansEmployeeWithoutIndex);
        assertThat(byStatus).noneMatch(PerformanceBudgetTest::scansEmployeeWithoutIndex);
        byName.forEach(step -> report("plan name prefix", step));
    }

    @Test
    void listQueriesStayWithinBudget() throws Exception {
        assertWithin("list, no filter, page 1", LIST_BUDGET_MS, API + "/employees?page=0&size=25");
        assertWithin("list, country+dept, sort salary", LIST_BUDGET_MS,
                API + "/employees?country=IN&department=2&sort=salary&direction=desc");
        assertWithin("list, name prefix search", LIST_BUDGET_MS, API + "/employees?search=Asha");
        assertWithin("list, deep page (400)", LIST_BUDGET_MS, API + "/employees?page=399&size=25&sort=salary");
    }

    @Test
    void insightsStayWithinBudget() throws Exception {
        assertWithin("insights summary", INSIGHT_BUDGET_MS, API + "/insights/summary");
        assertWithin("insights by country", INSIGHT_BUDGET_MS, API + "/insights/by/country");
        assertWithin("insights by jobTitle (widest)", INSIGHT_BUDGET_MS, API + "/insights/by/jobTitle");
        assertWithin("insights distribution", INSIGHT_BUDGET_MS, API + "/insights/distribution");
        assertWithin("insights top 10", INSIGHT_BUDGET_MS, API + "/insights/top?limit=10");
    }

    private void assertWithin(String label, double budgetMs, String url) throws Exception {
        for (int run = 0; run < WARMUP_RUNS; run++) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
        List<Double> timings = new ArrayList<>();
        for (int run = 0; run < MEASURED_RUNS; run++) {
            long started = System.nanoTime();
            mockMvc.perform(get(url)).andExpect(status().isOk());
            timings.add((System.nanoTime() - started) / 1_000_000.0);
        }
        timings.sort(Comparator.naturalOrder());
        double p95 = timings.get((int) Math.ceil(MEASURED_RUNS * 0.95) - 1);
        report(label, "p95 %.1f ms (median %.1f ms)".formatted(p95, timings.get(MEASURED_RUNS / 2)));
        assertThat(p95).as(label).isLessThan(budgetMs);
    }

    private List<String> queryPlan(String whereAndOrder) {
        String sql = "EXPLAIN QUERY PLAN SELECT e.id " + EmployeeSql.FROM_EMPLOYEE_JOINS + " " + whereAndOrder;
        return jdbcTemplate.queryForList(sql).stream().map(row -> String.valueOf(row.get("detail"))).toList();
    }

    private static boolean scansEmployeeWithoutIndex(String step) {
        return step.startsWith("SCAN e") && !step.contains("USING");
    }

    private static void report(String label, Object value) {
        System.out.println("[perf] " + label + ": " + value);
    }
}
