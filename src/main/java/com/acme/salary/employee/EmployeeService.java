package com.acme.salary.employee;

import com.acme.salary.common.PageResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummary> search(EmployeeSearchCriteria criteria) {
        long totalItems = employeeRepository.count(criteria);
        List<EmployeeSummary> items = totalItems == 0 ? List.of() : employeeRepository.search(criteria);
        return PageResponse.of(items, criteria.page(), criteria.size(), totalItems);
    }
}
