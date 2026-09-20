package com.acme.salary.salary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One salary change; amounts are in major units of {@code currency}.
 *
 * @param oldSalary previous salary, or null for the employee's first salary
 */
public record SalaryHistoryEntry(
        long id,
        BigDecimal oldSalary,
        BigDecimal newSalary,
        String currency,
        LocalDate effectiveDate,
        String reason,
        Instant changedAt) {
}
