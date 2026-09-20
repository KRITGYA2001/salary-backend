package com.acme.salary.insights;

import java.util.Arrays;

/** Allow-list of grouping dimensions; the SQL fragments are never taken from user input. */
public enum InsightDimension {
    COUNTRY("country", "co.code", "co.name"),
    DEPARTMENT("department", "CAST(d.id AS TEXT)", "d.name"),
    JOB_TITLE("jobTitle", "CAST(jt.id AS TEXT)", "jt.title");

    private final String parameterName;
    private final String keySql;
    private final String labelSql;

    InsightDimension(String parameterName, String keySql, String labelSql) {
        this.parameterName = parameterName;
        this.keySql = keySql;
        this.labelSql = labelSql;
    }

    public String keySql() {
        return keySql;
    }

    public String labelSql() {
        return labelSql;
    }

    /** @throws IllegalArgumentException if the value is not one of the supported dimensions */
    public static InsightDimension fromParameter(String value) {
        return Arrays.stream(values())
                .filter(dimension -> dimension.parameterName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported dimension: " + value));
    }
}
