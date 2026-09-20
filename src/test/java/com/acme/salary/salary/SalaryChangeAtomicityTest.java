package com.acme.salary.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmploymentType;
import com.acme.salary.employee.NewEmployee;
import com.acme.salary.seed.SeedRepository;
import com.acme.salary.support.AbstractIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** Proves the salary update and its history entry succeed or fail together. */
@AutoConfigureMockMvc
class SalaryChangeAtomicityTest extends AbstractIntegrationTest {

    private static final long ORIGINAL_SALARY_MINOR = 100_000_00L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    @MockitoSpyBean
    private SalaryHistoryRepository salaryHistoryRepository;

    @Test
    void salaryIsRolledBackWhenTheHistoryEntryCannotBeWritten() throws Exception {
        seedRepository.insertAll(List.of(new NewEmployee("EMP-00001", "Employee 1", "e1@acme.example", 1L, 1L,
                        "US", EmploymentType.FULL_TIME, ORIGINAL_SALARY_MINOR, LocalDate.of(2020, 1, 5),
                        EmployeeStatus.ACTIVE)),
                Instant.parse("2026-01-01T00:00:00Z"));
        long employeeId = jdbcTemplate.queryForObject("SELECT id FROM employee", Long.class);
        doThrow(new IllegalStateException("history write failed")).when(salaryHistoryRepository)
                .insert(anyLong(), anyLong(), anyLong(), anyString(), any(), anyString(), any());

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newSalary": 120000, "effectiveDate": "2025-04-01", "reason": "Raise"}"""))
                .andExpect(status().isInternalServerError());

        Long salaryMinor = jdbcTemplate.queryForObject("SELECT salary_minor FROM employee", Long.class);
        Long historyRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_history", Long.class);
        assertThat(salaryMinor).isEqualTo(ORIGINAL_SALARY_MINOR);
        assertThat(historyRows).isEqualTo(1L);
    }
}
