package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class EmployeeCrudApiTest extends AbstractIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/employees";

    @Autowired
    private MockMvc mockMvc;

    private long departmentId;
    private long jobTitleId;
    private long otherDepartmentId;

    @BeforeEach
    void lookUpReferenceIds() {
        Map<String, Object> jobTitle = jdbcTemplate.queryForMap("SELECT id, department_id FROM job_title LIMIT 1");
        jobTitleId = ((Number) jobTitle.get("id")).longValue();
        departmentId = ((Number) jobTitle.get("department_id")).longValue();
        otherDepartmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM department WHERE id <> ? LIMIT 1", Long.class, departmentId);
    }

    private String createBody(String email, String country, String salary) {
        return """
                {"fullName": " Priya Nair ", "email": "%s", "departmentId": %d, "jobTitleId": %d,
                 "countryCode": "%s", "employmentType": "FULL_TIME", "salary": %s, "hireDate": "2024-03-01"}"""
                .formatted(email, departmentId, jobTitleId, country, salary);
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(EMPLOYEES_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void createsEmployeeWithMinorUnitSalaryCodeAndInitialHistory() throws Exception {
        create(createBody("Priya@Acme.example", "in", "1500000.50"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.fullName").value("Priya Nair"))
                .andExpect(jsonPath("$.email").value("priya@acme.example"))
                .andExpect(jsonPath("$.countryCode").value("IN"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.salary").value(1500000.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.employeeCode").value(matchesPattern("EMP-\\d{5}")));

        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT old_salary_minor, new_salary_minor, currency, reason FROM salary_history");
        assertThat(history.get("old_salary_minor")).isNull();
        assertThat(((Number) history.get("new_salary_minor")).longValue()).isEqualTo(150_000_050L);
        assertThat(history).containsEntry("currency", "INR").containsEntry("reason", "Initial salary");
    }

    @Test
    void storesZeroDecimalCurrencySalaryWithoutScaling() throws Exception {
        create(createBody("kenji@acme.example", "JP", "6000000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salary").value(6000000));

        Long salaryMinor = jdbcTemplate.queryForObject("SELECT salary_minor FROM employee", Long.class);
        assertThat(salaryMinor).isEqualTo(6_000_000L);
    }

    @Test
    void rejectsInvalidFieldsWithPerFieldDetails() throws Exception {
        create("""
                {"fullName": "", "email": "not-an-email", "departmentId": 1, "jobTitleId": 1,
                 "countryCode": "USA", "employmentType": "FULL_TIME", "salary": -5, "hireDate": "2024-03-01"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.fullName").exists())
                .andExpect(jsonPath("$.error.details.email").exists())
                .andExpect(jsonPath("$.error.details.countryCode").exists())
                .andExpect(jsonPath("$.error.details.salary").exists());
    }

    @Test
    void rejectsUnknownCountryMismatchedDepartmentAndFutureHireDate() throws Exception {
        create(createBody("a@acme.example", "ZZ", "100")).andExpect(status().isBadRequest());
        create(createBody("b@acme.example", "US", "100").replace(
                "\"departmentId\": " + departmentId, "\"departmentId\": " + otherDepartmentId))
                .andExpect(status().isBadRequest());
        create(createBody("c@acme.example", "US", "100").replace("2024-03-01", "2999-01-01"))
                .andExpect(status().isBadRequest());
        create("{not json").andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateEmailWithConflict() throws Exception {
        create(createBody("dup@acme.example", "US", "100")).andExpect(status().isCreated());

        create(createBody("DUP@acme.example", "US", "200"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_history", Long.class)).isEqualTo(1L);
    }

    @Test
    void getReturnsEmployeeOr404() throws Exception {
        create(createBody("get@acme.example", "US", "100")).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM employee", Long.class);

        mockMvc.perform(get(EMPLOYEES_URL + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("get@acme.example"));
        mockMvc.perform(get(EMPLOYEES_URL + "/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(EMPLOYEES_URL + "/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void patchUpdatesOnlyProvidedFieldsAndLeavesSalaryAlone() throws Exception {
        create(createBody("patch@acme.example", "US", "100")).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM employee", Long.class);

        mockMvc.perform(patch(EMPLOYEES_URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Priya N.\", \"employmentType\": \"CONTRACT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya N."))
                .andExpect(jsonPath("$.employmentType").value("CONTRACT"))
                .andExpect(jsonPath("$.email").value("patch@acme.example"))
                .andExpect(jsonPath("$.salary").value(100.0));
    }

    @Test
    void patchRejectsBlankNameDepartmentMismatchAndUnknownEmployee() throws Exception {
        create(createBody("p2@acme.example", "US", "100")).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM employee", Long.class);

        mockMvc.perform(patch(EMPLOYEES_URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(EMPLOYEES_URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\": " + otherDepartmentId + "}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(EMPLOYEES_URL + "/999999").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivateIsIdempotentAndKeepsHistory() throws Exception {
        create(createBody("bye@acme.example", "US", "100")).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject("SELECT id FROM employee", Long.class);

        mockMvc.perform(post(EMPLOYEES_URL + "/" + id + "/deactivate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mockMvc.perform(post(EMPLOYEES_URL + "/" + id + "/deactivate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mockMvc.perform(post(EMPLOYEES_URL + "/999999/deactivate")).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_history", Long.class)).isEqualTo(1L);
    }
}
