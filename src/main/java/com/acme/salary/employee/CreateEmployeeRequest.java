package com.acme.salary.employee;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for hiring an employee.
 *
 * @param salary annual salary in major units of the country's currency
 */
public record CreateEmployeeRequest(
        @NotBlank @Size(max = EmployeeConstraints.MAX_NAME_LENGTH) String fullName,
        @NotBlank @Email @Size(max = EmployeeConstraints.MAX_EMAIL_LENGTH) String email,
        @NotNull @Positive Long departmentId,
        @NotNull @Positive Long jobTitleId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a 2-letter country code") String countryCode,
        @NotNull EmploymentType employmentType,
        @NotNull @Positive @Digits(integer = EmployeeConstraints.MAX_SALARY_INTEGER_DIGITS, fraction = 3)
        BigDecimal salary,
        @NotNull LocalDate hireDate) {
}
