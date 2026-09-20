package com.acme.salary.seed;

import static com.acme.salary.seed.SeedConstants.ACTIVE_PERCENTAGE;
import static com.acme.salary.seed.SeedConstants.BASE_SALARY_BY_COUNTRY;
import static com.acme.salary.seed.SeedConstants.EARLIEST_HIRE_DATE;
import static com.acme.salary.seed.SeedConstants.EMAIL_DOMAIN;
import static com.acme.salary.seed.SeedConstants.EMPLOYEE_CODE_FORMAT;
import static com.acme.salary.seed.SeedConstants.FIRST_NAMES;
import static com.acme.salary.seed.SeedConstants.FULL_TIME_PERCENTAGE;
import static com.acme.salary.seed.SeedConstants.LAST_NAMES;
import static com.acme.salary.seed.SeedConstants.LATEST_HIRE_OFFSET_DAYS;
import static com.acme.salary.seed.SeedConstants.MAX_SALARY_FACTOR;
import static com.acme.salary.seed.SeedConstants.MIN_SALARY_FACTOR;
import static com.acme.salary.seed.SeedConstants.PART_TIME_PERCENTAGE;

import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmploymentType;
import com.acme.salary.employee.NewEmployee;
import com.acme.salary.reference.Country;
import com.acme.salary.reference.Currency;
import com.acme.salary.reference.JobTitle;
import com.acme.salary.reference.ReferenceData;
import com.acme.salary.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Produces a reproducible stream of realistic employees: the same seed, reference data and reference
 * date always yield the same sequence.
 */
public class EmployeeGenerator {

    private final ReferenceData referenceData;
    private final Map<String, Currency> currenciesByCode;
    private final LocalDate latestHireDate;
    private final SplittableRandom random;

    public EmployeeGenerator(ReferenceData referenceData, long randomSeed, LocalDate today) {
        this.referenceData = referenceData;
        this.currenciesByCode = referenceData.currenciesByCode();
        this.latestHireDate = today.minusDays(LATEST_HIRE_OFFSET_DAYS);
        this.random = new SplittableRandom(randomSeed);
    }

    /** Generates the employee with the given 1-based sequence number; call in increasing order. */
    public NewEmployee next(int sequenceNumber) {
        String firstName = pick(FIRST_NAMES);
        String lastName = pick(LAST_NAMES);
        Country country = pick(referenceData.countries());
        JobTitle jobTitle = pick(referenceData.jobTitles());
        Currency currency = currenciesByCode.get(country.currencyCode());

        return new NewEmployee(
                EMPLOYEE_CODE_FORMAT.formatted(sequenceNumber),
                firstName + " " + lastName,
                "%s.%s.%d@%s".formatted(firstName, lastName, sequenceNumber, EMAIL_DOMAIN).toLowerCase(),
                jobTitle.departmentId(),
                jobTitle.id(),
                country.code(),
                pickEmploymentType(),
                pickSalaryMinor(country, currency),
                pickHireDate(),
                random.nextInt(100) < ACTIVE_PERCENTAGE ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE);
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private EmploymentType pickEmploymentType() {
        int roll = random.nextInt(100);
        if (roll < FULL_TIME_PERCENTAGE) {
            return EmploymentType.FULL_TIME;
        }
        return roll < FULL_TIME_PERCENTAGE + PART_TIME_PERCENTAGE ? EmploymentType.PART_TIME : EmploymentType.CONTRACT;
    }

    private long pickSalaryMinor(Country country, Currency currency) {
        double factor = MIN_SALARY_FACTOR + random.nextDouble() * (MAX_SALARY_FACTOR - MIN_SALARY_FACTOR);
        long baseMajor = BASE_SALARY_BY_COUNTRY.get(country.code());
        return MoneyUtils.toMinorUnits(BigDecimal.valueOf(baseMajor * factor), currency.minorUnitExponent());
    }

    private LocalDate pickHireDate() {
        return LocalDate.ofEpochDay(random.nextLong(EARLIEST_HIRE_DATE.toEpochDay(), latestHireDate.toEpochDay() + 1));
    }
}
