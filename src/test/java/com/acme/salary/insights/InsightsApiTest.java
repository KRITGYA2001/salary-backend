package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmploymentType;
import com.acme.salary.employee.NewEmployee;
import com.acme.salary.reference.ReferenceData;
import com.acme.salary.reference.ReferenceDataService;
import com.acme.salary.seed.EmployeeGenerator;
import com.acme.salary.seed.SeedRepository;
import com.acme.salary.support.AbstractIntegrationTest;
import com.acme.salary.util.PercentileUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class InsightsApiTest extends AbstractIntegrationTest {

    private static final String INSIGHTS_URL = "/api/v1/insights";
    private static final int GENERATED_EMPLOYEES = 1_000;
    private static final double MONEY_TOLERANCE = 0.01;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    @Autowired
    private InsightsRepository insightsRepository;

    @Autowired
    private ReferenceDataService referenceDataService;

    private static NewEmployee employee(
            int number, String country, EmployeeStatus status, long salaryMinor) {
        return new NewEmployee("EMP-%05d".formatted(number), "Person " + number, "p" + number + "@acme.example", 1L,
                1L, country, EmploymentType.FULL_TIME, salaryMinor, LocalDate.of(2020, 1, 1), status);
    }

    private void insert(NewEmployee... employees) {
        seedRepository.insertAll(List.of(employees), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void summaryMatchesHandComputedStatsAndIgnoresInactiveEmployees() throws Exception {
        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L),
                employee(2, "US", EmployeeStatus.ACTIVE, 20_000_00L),
                employee(3, "US", EmployeeStatus.ACTIVE, 30_000_00L),
                employee(4, "US", EmployeeStatus.ACTIVE, 40_000_00L),
                employee(5, "US", EmployeeStatus.INACTIVE, 900_000_00L));

        mockMvc.perform(get(INSIGHTS_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcount").value(4))
                .andExpect(jsonPath("$.averageUsd").value(25000.00))
                .andExpect(jsonPath("$.medianUsd").value(20000.00))
                .andExpect(jsonPath("$.p90Usd").value(40000.00))
                .andExpect(jsonPath("$.minUsd").value(10000.00))
                .andExpect(jsonPath("$.maxUsd").value(40000.00));
    }

    @Test
    void summaryOfNoEmployeesHasZeroHeadcountAndNullMoney() throws Exception {
        mockMvc.perform(get(INSIGHTS_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcount").value(0))
                .andExpect(jsonPath("$.medianUsd").doesNotExist());
    }

    @Test
    void summaryHonoursCountryFilter() throws Exception {
        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L),
                employee(2, "JP", EmployeeStatus.ACTIVE, 6_000_000L));

        mockMvc.perform(get(INSIGHTS_URL + "/summary").param("country", "us"))
                .andExpect(jsonPath("$.headcount").value(1))
                .andExpect(jsonPath("$.maxUsd").value(10000.00));
    }

    @Test
    void sqlPercentilesAgreeWithJavaNearestRankForEveryCountry() {
        ReferenceData referenceData = referenceDataService.load();
        EmployeeGenerator generator = new EmployeeGenerator(referenceData, 7L, LocalDate.of(2026, 1, 1));
        List<NewEmployee> generated = new ArrayList<>();
        for (int number = 1; number <= GENERATED_EMPLOYEES; number++) {
            generated.add(generator.next(number));
        }
        seedRepository.insertAll(generated, Instant.parse("2026-01-01T00:00:00Z"));

        Map<String, List<Double>> usdByCountry = generated.stream()
                .filter(employee -> employee.status() == EmployeeStatus.ACTIVE)
                .collect(Collectors.groupingBy(NewEmployee::countryCode,
                        Collectors.mapping(employee -> toUsd(referenceData, employee), Collectors.toList())));

        List<GroupSalaryStats> actual = insightsRepository.groupBy(
                InsightDimension.COUNTRY, new InsightsFilter(null, null, null));

        assertThat(actual).hasSize(usdByCountry.size());
        for (GroupSalaryStats group : actual) {
            List<Double> sorted = usdByCountry.get(group.key()).stream().sorted().toList();
            assertThat(group.stats().headcount()).isEqualTo(sorted.size());
            assertThat(group.stats().medianUsd().doubleValue())
                    .isCloseTo(PercentileUtils.nearestRank(sorted, 50), within(MONEY_TOLERANCE));
            assertThat(group.stats().p90Usd().doubleValue())
                    .isCloseTo(PercentileUtils.nearestRank(sorted, 90), within(MONEY_TOLERANCE));
            assertThat(group.stats().minUsd().doubleValue()).isCloseTo(sorted.get(0), within(MONEY_TOLERANCE));
        }
    }

    private static double toUsd(ReferenceData referenceData, NewEmployee employee) {
        var currency = referenceData.currencyOfCountry(employee.countryCode());
        BigDecimal major = BigDecimal.valueOf(employee.salaryMinor()).movePointLeft(currency.minorUnitExponent());
        return major.multiply(currency.usdPerUnit()).doubleValue();
    }

    @Test
    void groupsByDepartmentAndRejectsUnknownDimension() throws Exception {
        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L));

        mockMvc.perform(get(INSIGHTS_URL + "/by/department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stats.headcount").value(1));
        mockMvc.perform(get(INSIGHTS_URL + "/by/shoeSize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void distributionBucketsCoverEveryActiveEmployeeExactlyOnce() throws Exception {
        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L),
                employee(2, "US", EmployeeStatus.ACTIVE, 20_000_00L),
                employee(3, "US", EmployeeStatus.ACTIVE, 30_000_00L),
                employee(4, "US", EmployeeStatus.ACTIVE, 40_000_00L),
                employee(5, "US", EmployeeStatus.INACTIVE, 90_000_00L));

        mockMvc.perform(get(INSIGHTS_URL + "/distribution").param("buckets", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].count").value(1))
                .andExpect(jsonPath("$[1].count").value(1))
                .andExpect(jsonPath("$[2].count").value(2))
                .andExpect(jsonPath("$[2].toUsd").value(40000.00));
    }

    @Test
    void distributionHandlesEmptyAndSingleValueGroups() throws Exception {
        mockMvc.perform(get(INSIGHTS_URL + "/distribution"))
                .andExpect(jsonPath("$.length()").value(0));

        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L),
                employee(2, "US", EmployeeStatus.ACTIVE, 10_000_00L));
        mockMvc.perform(get(INSIGHTS_URL + "/distribution"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void topReturnsHighestOrLowestActiveEarnersInUsd() throws Exception {
        insert(employee(1, "US", EmployeeStatus.ACTIVE, 10_000_00L),
                employee(2, "JP", EmployeeStatus.ACTIVE, 6_000_000L),
                employee(3, "US", EmployeeStatus.ACTIVE, 30_000_00L),
                employee(4, "US", EmployeeStatus.INACTIVE, 900_000_00L));

        mockMvc.perform(get(INSIGHTS_URL + "/top").param("limit", "2"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Person 2"))
                .andExpect(jsonPath("$[1].fullName").value("Person 3"));
        mockMvc.perform(get(INSIGHTS_URL + "/top").param("direction", "lowest").param("limit", "1"))
                .andExpect(jsonPath("$[0].fullName").value("Person 1"));
    }

    @Test
    void rejectsOutOfRangeLimitBucketsAndDirection() throws Exception {
        mockMvc.perform(get(INSIGHTS_URL + "/top").param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get(INSIGHTS_URL + "/top").param("limit", "51")).andExpect(status().isBadRequest());
        mockMvc.perform(get(INSIGHTS_URL + "/top").param("direction", "sideways"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(INSIGHTS_URL + "/distribution").param("buckets", "51"))
                .andExpect(status().isBadRequest());
    }
}
