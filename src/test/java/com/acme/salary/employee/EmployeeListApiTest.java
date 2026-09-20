package com.acme.salary.employee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.seed.SeedRepository;
import com.acme.salary.support.AbstractIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class EmployeeListApiTest extends AbstractIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/employees";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    @BeforeEach
    void insertEmployees() {
        seedRepository.insertAll(List.of(
                employee(1, "Asha Rao", "IN", 1L, EmployeeStatus.ACTIVE, 1_200_000_00L),
                employee(2, "Bob Smith", "US", 1L, EmployeeStatus.ACTIVE, 90_000_00L),
                employee(3, "Carla Jones", "US", 2L, EmployeeStatus.INACTIVE, 120_000_00L),
                employee(4, "Akira 100%", "JP", 2L, EmployeeStatus.ACTIVE, 6_000_000L)),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static NewEmployee employee(
            int number, String name, String country, long departmentId, EmployeeStatus status, long salaryMinor) {
        return new NewEmployee("EMP-%05d".formatted(number), name, "user" + number + "@acme.example", departmentId, 1L,
                country, EmploymentType.FULL_TIME, salaryMinor, LocalDate.of(2020, 1, number), status);
    }

    @Test
    void returnsAllEmployeesSortedByNameWithPagingTotals() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Akira 100%"))
                .andExpect(jsonPath("$.items[3].fullName").value("Carla Jones"));
    }

    @Test
    void filtersByCountryDepartmentAndStatus() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).param("country", "us").param("department", "1"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Bob Smith"));
        mockMvc.perform(get(EMPLOYEES_URL).param("status", "INACTIVE"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Carla Jones"));
    }

    @Test
    void searchMatchesNameCodeOrEmailPrefixCaseInsensitivelyAndEscapesWildcards() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).param("search", "bo"))
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(get(EMPLOYEES_URL).param("search", "EMP-00003"))
                .andExpect(jsonPath("$.items[0].fullName").value("Carla Jones"));
        mockMvc.perform(get(EMPLOYEES_URL).param("search", "user2@"))
                .andExpect(jsonPath("$.items[0].fullName").value("Bob Smith"));
        mockMvc.perform(get(EMPLOYEES_URL).param("search", "%"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void sortsBySalaryInUsdAcrossCurrencies() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).param("sort", "salary").param("direction", "desc"))
                .andExpect(jsonPath("$.items[0].fullName").value("Carla Jones"))
                .andExpect(jsonPath("$.items[0].salary").value(120000.00))
                .andExpect(jsonPath("$.items[0].currency").value("USD"));
    }

    @Test
    void paginatesResults() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).param("size", "3").param("page", "1"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void rejectsUnsupportedSortFieldOversizedPageAndBadStatus() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
        mockMvc.perform(get(EMPLOYEES_URL).param("size", "101")).andExpect(status().isBadRequest());
        mockMvc.perform(get(EMPLOYEES_URL).param("status", "FIRED")).andExpect(status().isBadRequest());
    }
}
