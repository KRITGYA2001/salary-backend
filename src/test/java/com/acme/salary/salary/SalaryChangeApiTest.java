package com.acme.salary.salary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmploymentType;
import com.acme.salary.employee.NewEmployee;
import com.acme.salary.seed.SeedRepository;
import com.acme.salary.support.AbstractIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class SalaryChangeApiTest extends AbstractIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/employees/";
    private static final LocalDate HIRE_DATE = LocalDate.of(2020, 1, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    private long activeEmployeeId;
    private long inactiveEmployeeId;

    @BeforeEach
    void insertEmployees() {
        seedRepository.insertAll(List.of(
                employee(1, EmployeeStatus.ACTIVE), employee(2, EmployeeStatus.INACTIVE)),
                Instant.parse("2026-01-01T00:00:00Z"));
        activeEmployeeId = idOf("EMP-00001");
        inactiveEmployeeId = idOf("EMP-00002");
    }

    private long idOf(String employeeCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM employee WHERE employee_code = ?", Long.class, employeeCode);
    }

    private static NewEmployee employee(int number, EmployeeStatus status) {
        return new NewEmployee("EMP-%05d".formatted(number), "Employee " + number,
                "employee" + number + "@acme.example", 1L, 1L, "US", EmploymentType.FULL_TIME,
                100_000_00L, HIRE_DATE, status);
    }

    private ResultActions changeSalary(long employeeId, String newSalary, String effectiveDate, String reason)
            throws Exception {
        String body = """
                {"newSalary": %s, "effectiveDate": "%s", "reason": "%s"}""".formatted(newSalary, effectiveDate, reason);
        return mockMvc.perform(post(EMPLOYEES_URL + employeeId + "/salary")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void changesSalaryAndRecordsHistoryNewestFirst() throws Exception {
        changeSalary(activeEmployeeId, "110000.50", "2025-04-01", "  Annual review ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(110000.50));

        mockMvc.perform(get(EMPLOYEES_URL + activeEmployeeId + "/salary-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].oldSalary").value(100000.00))
                .andExpect(jsonPath("$[0].newSalary").value(110000.50))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].effectiveDate").value("2025-04-01"))
                .andExpect(jsonPath("$[0].reason").value("Annual review"))
                .andExpect(jsonPath("$[1].oldSalary").doesNotExist())
                .andExpect(jsonPath("$[1].reason").value("Initial salary"));
    }

    @Test
    void rejectsInactiveEmployeeWithConflict() throws Exception {
        changeSalary(inactiveEmployeeId, "120000", "2025-04-01", "Raise")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void rejectsUnchangedSalaryIncludingValuesThatRoundToTheSame() throws Exception {
        changeSalary(activeEmployeeId, "100000", "2025-04-01", "No change").andExpect(status().isBadRequest());
        changeSalary(activeEmployeeId, "100000.004", "2025-04-01", "No change").andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEffectiveDateBeforeHireOrInTheFuture() throws Exception {
        changeSalary(activeEmployeeId, "120000", "2019-12-31", "Backdated").andExpect(status().isBadRequest());
        changeSalary(activeEmployeeId, "120000", "2999-01-01", "Scheduled").andExpect(status().isBadRequest());
    }

    @Test
    void acceptsEffectiveDateEqualToHireDate() throws Exception {
        changeSalary(activeEmployeeId, "120000", HIRE_DATE.toString(), "Correction").andExpect(status().isOk());
    }

    @Test
    void rejectsMissingReasonAndNonPositiveSalary() throws Exception {
        changeSalary(activeEmployeeId, "0", "2025-04-01", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.reason").exists())
                .andExpect(jsonPath("$.error.details.newSalary").exists());
    }

    @Test
    void returnsNotFoundForUnknownEmployee() throws Exception {
        changeSalary(999_999, "120000", "2025-04-01", "Raise").andExpect(status().isNotFound());
        mockMvc.perform(get(EMPLOYEES_URL + "999999/salary-history")).andExpect(status().isNotFound());
    }
}
