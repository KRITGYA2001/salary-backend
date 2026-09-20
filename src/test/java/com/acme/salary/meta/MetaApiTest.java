package com.acme.salary.meta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MetaApiTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCountriesWithCurrencyDepartmentsAndJobTitlesWithTheirDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/meta/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries[?(@.code=='JP')].currencyCode").value("JPY"))
                .andExpect(jsonPath("$.departments.length()").isNotEmpty())
                .andExpect(jsonPath("$.jobTitles[0].departmentId").isNumber());
    }
}
