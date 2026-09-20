package com.acme.salary.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Partial update of non-salary fields; omitted (null) fields keep their current value.
 * Country and salary have dedicated workflows and cannot be changed here.
 */
public record UpdateEmployeeRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = EmployeeConstraints.MAX_NAME_LENGTH) String fullName,
        @Email @Size(max = EmployeeConstraints.MAX_EMAIL_LENGTH) String email,
        @Positive Long departmentId,
        @Positive Long jobTitleId,
        EmploymentType employmentType) {
}
