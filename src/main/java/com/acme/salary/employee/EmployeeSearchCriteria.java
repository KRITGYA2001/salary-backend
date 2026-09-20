package com.acme.salary.employee;

/**
 * Validated filter, sort and paging options for the employee list. Null filters are ignored.
 *
 * @param search prefix matched against full name, employee code and email
 */
public record EmployeeSearchCriteria(
        String countryCode,
        Long departmentId,
        Long jobTitleId,
        EmployeeStatus status,
        String search,
        EmployeeSortField sortField,
        SortDirection sortDirection,
        int page,
        int size) {

    public int offset() {
        return page * size;
    }
}
