package com.acme.salary.employee;

import java.time.LocalDate;

/** Data required to insert an employee; the salary is in the minor units of the country's currency. */
public record NewEmployee(
        String employeeCode,
        String fullName,
        String email,
        long departmentId,
        long jobTitleId,
        String countryCode,
        EmploymentType employmentType,
        long salaryMinor,
        LocalDate hireDate,
        EmployeeStatus status) {
}
