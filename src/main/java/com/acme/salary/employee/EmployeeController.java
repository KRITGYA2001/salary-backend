package com.acme.salary.employee;

import static com.acme.salary.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.acme.salary.common.PaginationConstants.MAX_PAGE_SIZE;

import com.acme.salary.common.ApiPaths;
import com.acme.salary.common.PageResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.EMPLOYEES)
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Lists employees with optional filters, prefix search, sorting and pagination.
     * Page sizes above {@link com.acme.salary.common.PaginationConstants#MAX_PAGE_SIZE} are rejected.
     */
    @GetMapping
    public PageResponse<EmployeeSummary> list(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long jobTitle,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                StringUtils.hasText(country) ? country.toUpperCase() : null,
                department,
                jobTitle,
                status,
                StringUtils.hasText(search) ? search : null,
                EmployeeSortField.fromParameter(sort),
                SortDirection.fromParameter(direction),
                page,
                size);
        return employeeService.search(criteria);
    }
}
