package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.employee.NewEmployee;
import com.acme.salary.reference.Country;
import com.acme.salary.reference.Currency;
import com.acme.salary.reference.Department;
import com.acme.salary.reference.JobTitle;
import com.acme.salary.reference.ReferenceData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class EmployeeGeneratorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);
    private static final int SAMPLE_SIZE = 2_000;

    private final ReferenceData referenceData = new ReferenceData(
            List.of(new Country("US", "United States", "USD"), new Country("JP", "Japan", "JPY")),
            List.of(new Currency("USD", 2, new BigDecimal("1.0")), new Currency("JPY", 0, new BigDecimal("0.0067"))),
            List.of(new Department(1, "Engineering")),
            List.of(new JobTitle(1, "Software Engineer", 1), new JobTitle(2, "QA Engineer", 1)));

    private List<NewEmployee> generate(long randomSeed) {
        EmployeeGenerator generator = new EmployeeGenerator(referenceData, randomSeed, TODAY);
        return IntStream.rangeClosed(1, SAMPLE_SIZE).mapToObj(generator::next).toList();
    }

    @Test
    void sameSeedProducesIdenticalEmployees() {
        assertThat(generate(7L)).isEqualTo(generate(7L));
    }

    @Test
    void differentSeedsProduceDifferentEmployees() {
        assertThat(generate(7L)).isNotEqualTo(generate(8L));
    }

    @Test
    void employeeCodesAndEmailsAreUnique() {
        List<NewEmployee> employees = generate(7L);

        Set<String> codes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        employees.forEach(employee -> {
            codes.add(employee.employeeCode());
            emails.add(employee.email());
        });
        assertThat(codes).hasSize(SAMPLE_SIZE);
        assertThat(emails).hasSize(SAMPLE_SIZE);
    }

    @Test
    void hireDatesFallWithinTheAllowedWindow() {
        assertThat(generate(7L)).allSatisfy(employee -> assertThat(employee.hireDate())
                .isBetween(SeedConstants.EARLIEST_HIRE_DATE, TODAY.minusDays(SeedConstants.LATEST_HIRE_OFFSET_DAYS)));
    }

    @Test
    void salariesArePositiveAndJobTitleMatchesDepartment() {
        assertThat(generate(7L)).allSatisfy(employee -> {
            assertThat(employee.salaryMinor()).isPositive();
            assertThat(employee.departmentId()).isEqualTo(1L);
        });
    }

    @Test
    void mostEmployeesAreActive() {
        long activeCount = generate(7L).stream()
                .filter(employee -> employee.status().name().equals("ACTIVE"))
                .count();

        assertThat(activeCount).isBetween((long) (SAMPLE_SIZE * 0.9), (long) SAMPLE_SIZE);
    }
}
