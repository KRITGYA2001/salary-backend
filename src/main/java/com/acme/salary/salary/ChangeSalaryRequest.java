package com.acme.salary.salary;

import com.acme.salary.employee.EmployeeConstraints;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for changing an employee's salary.
 *
 * @param newSalary new annual salary in major units of the employee's currency
 */
public record ChangeSalaryRequest(
        @NotNull @Positive @Digits(integer = EmployeeConstraints.MAX_SALARY_INTEGER_DIGITS, fraction = 3)
        BigDecimal newSalary,
        @NotNull LocalDate effectiveDate,
        @NotBlank @Size(max = SalaryConstants.MAX_REASON_LENGTH) String reason) {
}
