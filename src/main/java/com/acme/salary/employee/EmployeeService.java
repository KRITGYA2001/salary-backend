package com.acme.salary.employee;

import static com.acme.salary.salary.SalaryConstants.INITIAL_SALARY_REASON;

import com.acme.salary.common.PageResponse;
import com.acme.salary.common.error.InvalidRequestException;
import com.acme.salary.common.error.NotFoundException;
import com.acme.salary.reference.Currency;
import com.acme.salary.reference.JobTitle;
import com.acme.salary.reference.ReferenceData;
import com.acme.salary.reference.ReferenceDataService;
import com.acme.salary.salary.SalaryHistoryRepository;
import com.acme.salary.util.MoneyUtils;
import java.io.Writer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final ReferenceDataService referenceDataService;
    private final Clock clock;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            SalaryHistoryRepository salaryHistoryRepository,
            ReferenceDataService referenceDataService,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.referenceDataService = referenceDataService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummary> search(EmployeeSearchCriteria criteria) {
        long totalItems = employeeRepository.count(criteria);
        List<EmployeeSummary> items = totalItems == 0 ? List.of() : employeeRepository.search(criteria);
        return PageResponse.of(items, criteria.page(), criteria.size(), totalItems);
    }

    /** Writes all matching employees to {@code writer} as CSV without materialising the full result. */
    @Transactional(readOnly = true)
    public void exportCsv(EmployeeSearchCriteria criteria, Writer writer) {
        EmployeeCsvWriter csvWriter = new EmployeeCsvWriter(writer);
        csvWriter.writeHeader();
        employeeRepository.forEachMatching(criteria, csvWriter::writeRow);
    }

    @Transactional(readOnly = true)
    public EmployeeSummary get(long id) {
        return employeeRepository.findById(id).orElseThrow(() -> employeeNotFound(id));
    }

    /** Hires an employee and records the starting salary as the first history entry, atomically. */
    @Transactional
    public EmployeeSummary create(CreateEmployeeRequest request) {
        ReferenceData referenceData = referenceDataService.load();
        String countryCode = request.countryCode().toUpperCase(Locale.ROOT);
        Currency currency = referenceData.findCountry(countryCode)
                .map(country -> referenceData.currenciesByCode().get(country.currencyCode()))
                .orElseThrow(() -> new InvalidRequestException("Unknown country: " + countryCode));
        requireJobTitleInDepartment(referenceData, request.jobTitleId(), request.departmentId());
        if (request.hireDate().isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("hireDate must not be in the future");
        }

        long salaryMinor = MoneyUtils.toMinorUnits(request.salary(), currency.minorUnitExponent());
        Instant now = Instant.now(clock);
        NewEmployee newEmployee = new NewEmployee(
                null,
                request.fullName().trim(),
                normalizeEmail(request.email()),
                request.departmentId(),
                request.jobTitleId(),
                countryCode,
                request.employmentType(),
                salaryMinor,
                request.hireDate(),
                EmployeeStatus.ACTIVE);
        long id = employeeRepository.insert(newEmployee, now);
        salaryHistoryRepository.insert(
                id, null, salaryMinor, currency.code(), request.hireDate(), INITIAL_SALARY_REASON, now);
        return get(id);
    }

    /** Updates the non-salary fields that are present in the request. */
    @Transactional
    public EmployeeSummary update(long id, UpdateEmployeeRequest request) {
        EmployeeSummary current = get(id);
        long departmentId = request.departmentId() != null ? request.departmentId() : current.departmentId();
        long jobTitleId = request.jobTitleId() != null ? request.jobTitleId() : current.jobTitleId();
        if (request.departmentId() != null || request.jobTitleId() != null) {
            requireJobTitleInDepartment(referenceDataService.load(), jobTitleId, departmentId);
        }

        employeeRepository.updateProfile(
                id,
                request.fullName() != null ? request.fullName().trim() : current.fullName(),
                request.email() != null ? normalizeEmail(request.email()) : current.email(),
                departmentId,
                jobTitleId,
                request.employmentType() != null ? request.employmentType() : current.employmentType(),
                Instant.now(clock));
        return get(id);
    }

    /** Soft-deletes the employee so salary history stays intact; deactivating twice is harmless. */
    @Transactional
    public EmployeeSummary deactivate(long id) {
        EmployeeSummary current = get(id);
        if (current.status() == EmployeeStatus.ACTIVE) {
            employeeRepository.markInactive(id, Instant.now(clock));
        }
        return get(id);
    }

    private static void requireJobTitleInDepartment(ReferenceData referenceData, long jobTitleId, long departmentId) {
        referenceData.findDepartment(departmentId)
                .orElseThrow(() -> new InvalidRequestException("Unknown department: " + departmentId));
        JobTitle jobTitle = referenceData.findJobTitle(jobTitleId)
                .orElseThrow(() -> new InvalidRequestException("Unknown job title: " + jobTitleId));
        if (jobTitle.departmentId() != departmentId) {
            throw new InvalidRequestException(
                    "Job title '%s' does not belong to department %d".formatted(jobTitle.title(), departmentId));
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static NotFoundException employeeNotFound(long id) {
        return new NotFoundException("Employee %d not found".formatted(id));
    }
}
