package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.seed.SeedRepository;
import com.acme.salary.support.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class EmployeeExportApiTest extends AbstractIntegrationTest {

    private static final String EXPORT_URL = "/api/v1/employees/export.csv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedRepository seedRepository;

    @BeforeEach
    void insertEmployees() {
        seedRepository.insertAll(List.of(
                employee(1, "Asha Rao", "IN", EmployeeStatus.ACTIVE, 1_200_000_00L),
                employee(2, "=HYPERLINK(\"x\")", "US", EmployeeStatus.ACTIVE, 90_000_00L),
                employee(3, "Jones, Carla", "US", EmployeeStatus.INACTIVE, 120_000_00L)),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static NewEmployee employee(int number, String name, String country, EmployeeStatus status, long salary) {
        return new NewEmployee("EMP-%05d".formatted(number), name, "user" + number + "@acme.example", 1L, 1L,
                country, EmploymentType.FULL_TIME, salary, LocalDate.of(2020, 1, number), status);
    }

    private List<String> exportLines(String... params) throws Exception {
        var request = get(EXPORT_URL);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", EmployeeCsvWriter.CONTENT_DISPOSITION))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return List.of(body.split("\r\n"));
    }

    @Test
    void exportsHeaderAndEveryRowWithSalaryInLocalCurrencyAndUsd() throws Exception {
        List<String> lines = exportLines();

        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).startsWith("﻿Employee Code,Full Name,Email");
        assertThat(lines).anySatisfy(line -> assertThat(line)
                .startsWith("EMP-00001,Asha Rao,user1@acme.example,")
                .contains(",INR,FULL_TIME,1200000.00,"));
    }

    @Test
    void escapesCommasAndNeutralisesFormulas() throws Exception {
        List<String> lines = exportLines();

        assertThat(lines).anyMatch(line -> line.contains("\"Jones, Carla\""));
        assertThat(lines).anyMatch(line -> line.contains("\"'=HYPERLINK(\"\"x\"\")\""));
    }

    @Test
    void appliesTheSameFiltersAsTheList() throws Exception {
        assertThat(exportLines("country", "US", "status", "ACTIVE")).hasSize(2);
        assertThat(exportLines("country", "ZZ")).hasSize(1);
    }

    @Test
    void rejectsInvalidSortField() throws Exception {
        mockMvc.perform(get(EXPORT_URL).param("sort", "password")).andExpect(status().isBadRequest());
    }
}
