package com.acme.salary.insights;

import com.acme.salary.employee.SortDirection;
import java.util.Arrays;

/** Which end of the pay scale a "top earners" query returns. */
public enum TopDirection {
    HIGHEST("highest", SortDirection.DESC),
    LOWEST("lowest", SortDirection.ASC);

    private final String parameterName;
    private final SortDirection sortDirection;

    TopDirection(String parameterName, SortDirection sortDirection) {
        this.parameterName = parameterName;
        this.sortDirection = sortDirection;
    }

    public SortDirection sortDirection() {
        return sortDirection;
    }

    /** @throws IllegalArgumentException if the value is not one of the supported directions */
    public static TopDirection fromParameter(String value) {
        return Arrays.stream(values())
                .filter(direction -> direction.parameterName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported direction: " + value));
    }
}
