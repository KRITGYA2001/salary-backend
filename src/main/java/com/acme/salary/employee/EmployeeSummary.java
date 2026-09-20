package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Employee as shown in list and detail views.
 *
 * @param salary    annual salary in major units of {@code currency}
 * @param salaryUsd the same salary converted to US dollars for cross-country comparison
 */
public record EmployeeSummary(
        long id,
        String employeeCode,
        String fullName,
        String email,
        long departmentId,
        String department,
        long jobTitleId,
        String jobTitle,
        String countryCode,
        String countryName,
        String currency,
        EmploymentType employmentType,
        BigDecimal salary,
        BigDecimal salaryUsd,
        LocalDate hireDate,
        EmployeeStatus status) {
}
