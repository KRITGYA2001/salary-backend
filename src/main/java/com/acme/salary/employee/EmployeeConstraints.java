package com.acme.salary.employee;

/** Field limits shared by request validation. */
public final class EmployeeConstraints {

    public static final String EMPLOYEE_CODE_FORMAT = "EMP-%05d";
    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_SALARY_INTEGER_DIGITS = 12;

    private EmployeeConstraints() {
    }
}
