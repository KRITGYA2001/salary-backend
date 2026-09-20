package com.acme.salary.reference;

import java.math.BigDecimal;

/**
 * A currency with its minor-unit precision and a static USD conversion rate.
 *
 * @param code                ISO-4217 code, for example {@code INR}
 * @param minorUnitExponent   decimal places of the minor unit (USD 2, JPY 0)
 * @param usdPerUnit          value of one major unit in US dollars
 */
public record Currency(String code, int minorUnitExponent, BigDecimal usdPerUnit) {
}
