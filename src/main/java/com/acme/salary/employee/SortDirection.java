package com.acme.salary.employee;

public enum SortDirection {
    ASC,
    DESC;

    /** @throws IllegalArgumentException if the value is neither {@code asc} nor {@code desc} */
    public static SortDirection fromParameter(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported sort direction: " + value);
        }
    }
}
