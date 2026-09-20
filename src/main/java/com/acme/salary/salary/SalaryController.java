package com.acme.salary.salary;

import com.acme.salary.common.ApiPaths;
import com.acme.salary.employee.EmployeeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.EMPLOYEES + "/{employeeId}")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    /** Changes the salary and returns the updated employee. */
    @PostMapping("/salary")
    public EmployeeSummary changeSalary(
            @PathVariable long employeeId, @Valid @RequestBody ChangeSalaryRequest request) {
        return salaryService.changeSalary(employeeId, request);
    }

    /** Lists the employee's salary changes, newest first. */
    @GetMapping("/salary-history")
    public List<SalaryHistoryEntry> history(@PathVariable long employeeId) {
        return salaryService.history(employeeId);
    }
}
