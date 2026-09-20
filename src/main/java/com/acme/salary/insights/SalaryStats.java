package com.acme.salary.insights;

import java.math.BigDecimal;

/**
 * Pay statistics of a group of active employees, normalised to US dollars. Percentiles use the
 * nearest-rank method. All money fields are null when the group is empty.
 */
public record SalaryStats(
        long headcount,
        BigDecimal averageUsd,
        BigDecimal medianUsd,
        BigDecimal p90Usd,
        BigDecimal minUsd,
        BigDecimal maxUsd) {

    public static final SalaryStats EMPTY = new SalaryStats(0, null, null, null, null, null);
}
