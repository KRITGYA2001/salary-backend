package com.acme.salary.reference;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenceDataRepository {

    private final JdbcClient jdbcClient;

    public ReferenceDataRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Country> findAllCountries() {
        return jdbcClient.sql("SELECT code, name, currency FROM country ORDER BY name")
                .query((rs, rowNum) -> new Country(rs.getString("code"), rs.getString("name"), rs.getString("currency")))
                .list();
    }

    public List<Currency> findAllCurrencies() {
        return jdbcClient.sql("SELECT code, minor_unit_exponent, usd_per_unit FROM currency ORDER BY code")
                .query((rs, rowNum) -> new Currency(
                        rs.getString("code"), rs.getInt("minor_unit_exponent"), rs.getBigDecimal("usd_per_unit")))
                .list();
    }

    public List<Department> findAllDepartments() {
        return jdbcClient.sql("SELECT id, name FROM department ORDER BY name")
                .query((rs, rowNum) -> new Department(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    public List<JobTitle> findAllJobTitles() {
        return jdbcClient.sql("SELECT id, title, department_id FROM job_title ORDER BY title")
                .query((rs, rowNum) -> new JobTitle(rs.getLong("id"), rs.getString("title"), rs.getLong("department_id")))
                .list();
    }
}
