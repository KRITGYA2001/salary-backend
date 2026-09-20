package com.acme.salary.util;

import java.util.List;

/** Percentile helpers using the nearest-rank method (the same rule the SQL analytics use). */
public final class PercentileUtils {

    private static final int MEDIAN_PERCENTILE = 50;

    private PercentileUtils() {
    }

    /**
     * Returns the value at the given percentile of an already sorted, non-empty list.
     * The rank is {@code ceil(percentile / 100 * n)}, so p50 of an even-sized list is the lower middle value.
     *
     * @throws IllegalArgumentException if the list is empty or the percentile is outside 1..100
     */
    public static <T> T nearestRank(List<T> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            throw new IllegalArgumentException("Values must not be empty");
        }
        if (percentile < 1 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 1 and 100");
        }
        int rank = (int) Math.ceil(percentile / 100.0 * sortedValues.size());
        return sortedValues.get(rank - 1);
    }

    /** Returns the median (nearest-rank p50) of an already sorted, non-empty list. */
    public static <T> T median(List<T> sortedValues) {
        return nearestRank(sortedValues, MEDIAN_PERCENTILE);
    }
}
