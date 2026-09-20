package com.acme.salary.salary;

import com.acme.salary.common.error.ConflictException;
import com.acme.salary.common.error.InvalidRequestException;
import com.acme.salary.common.error.NotFoundException;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmployeeSummary;
import com.acme.salary.employee.SalaryContext;
import com.acme.salary.util.MoneyUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final Clock clock;

    public SalaryService(
            EmployeeRepository employeeRepository, SalaryHistoryRepository salaryHistoryRepository, Clock clock) {
        this.employeeRepository = employeeRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.clock = clock;
    }

    /**
     * Changes an active employee's salary and appends the history entry in one transaction.
     *
     * @throws NotFoundException       if the employee does not exist
     * @throws ConflictException       if the employee is inactive or the salary changed concurrently
     * @throws InvalidRequestException if the salary is unchanged or the effective date is before the hire date
     *                                 or in the future
     */
    @Transactional
    public EmployeeSummary changeSalary(long employeeId, ChangeSalaryRequest request) {
        SalaryContext context = requireContext(employeeId);
        if (context.status() != EmployeeStatus.ACTIVE) {
            throw new ConflictException("Salary of an inactive employee cannot be changed");
        }
        validateEffectiveDate(request.effectiveDate(), context.hireDate());

        long newSalaryMinor = MoneyUtils.toMinorUnits(request.newSalary(), context.minorUnitExponent());
        if (newSalaryMinor == context.salaryMinor()) {
            throw new InvalidRequestException("New salary is the same as the current salary");
        }

        Instant now = Instant.now(clock);
        if (!employeeRepository.updateSalary(employeeId, context.salaryMinor(), newSalaryMinor, now)) {
            throw new ConflictException("Salary was changed by someone else, please reload and retry");
        }
        salaryHistoryRepository.insert(employeeId, context.salaryMinor(), newSalaryMinor, context.currencyCode(),
                request.effectiveDate(), request.reason().trim(), now);
        return employeeRepository.findById(employeeId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<SalaryHistoryEntry> history(long employeeId) {
        requireContext(employeeId);
        return salaryHistoryRepository.findByEmployeeId(employeeId);
    }

    private SalaryContext requireContext(long employeeId) {
        return employeeRepository.findSalaryContext(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee %d not found".formatted(employeeId)));
    }

    private void validateEffectiveDate(LocalDate effectiveDate, LocalDate hireDate) {
        if (effectiveDate.isBefore(hireDate)) {
            throw new InvalidRequestException("effectiveDate must not be before the hire date " + hireDate);
        }
        if (effectiveDate.isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("effectiveDate must not be in the future");
        }
    }
}
