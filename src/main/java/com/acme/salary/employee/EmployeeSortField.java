package com.acme.salary.employee;

import java.util.Arrays;

/** Allow-list of sortable columns; the SQL expression is never taken from user input. */
public enum EmployeeSortField {
    NAME("name", "e.full_name COLLATE NOCASE"),
    SALARY("salary", EmployeeSql.SALARY_USD),
    HIRE_DATE("hireDate", "e.hire_date"),
    COUNTRY("country", "co.name");

    private final String parameterName;
    private final String sqlExpression;

    EmployeeSortField(String parameterName, String sqlExpression) {
        this.parameterName = parameterName;
        this.sqlExpression = sqlExpression;
    }

    public String sqlExpression() {
        return sqlExpression;
    }

    /** @throws IllegalArgumentException if the value is not one of the supported sort fields */
    public static EmployeeSortField fromParameter(String value) {
        return Arrays.stream(values())
                .filter(field -> field.parameterName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sort field: " + value));
    }
}
