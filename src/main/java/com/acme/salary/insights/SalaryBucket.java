package com.acme.salary.insights;

import java.math.BigDecimal;

/** One histogram bar covering {@code [fromUsd, toUsd)}; the last bucket also includes its upper bound. */
public record SalaryBucket(BigDecimal fromUsd, BigDecimal toUsd, long count) {
}
