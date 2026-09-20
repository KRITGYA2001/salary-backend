package com.acme.salary.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversions between human-readable amounts and the integer minor units (cents, paise, yen)
 * that the database stores. Floating point is never used for money.
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /**
     * Converts a major-unit amount (for example {@code 12345.67}) to minor units ({@code 1234567}),
     * rounding half up to the currency's precision.
     *
     * @throws IllegalArgumentException if the amount is null or not positive
     */
    public static long toMinorUnits(BigDecimal majorAmount, int minorUnitExponent) {
        if (majorAmount == null || majorAmount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return majorAmount
                .setScale(minorUnitExponent, RoundingMode.HALF_UP)
                .movePointRight(minorUnitExponent)
                .longValueExact();
    }

    /** Converts minor units back to a major-unit amount with the currency's scale. */
    public static BigDecimal toMajorUnits(long minorAmount, int minorUnitExponent) {
        return BigDecimal.valueOf(minorAmount, minorUnitExponent);
    }
}
