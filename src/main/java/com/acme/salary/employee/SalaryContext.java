package com.acme.salary.employee;

import java.time.LocalDate;

/** The facts about an employee needed to validate and apply a salary change. */
public record SalaryContext(
        long salaryMinor,
        String currencyCode,
        int minorUnitExponent,
        EmployeeStatus status,
        LocalDate hireDate) {
}
