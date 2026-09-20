package com.acme.salary.reference;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable snapshot of the small lookup tables used by validation, seeding and filters. */
public record ReferenceData(
        List<Country> countries,
        List<Currency> currencies,
        List<Department> departments,
        List<JobTitle> jobTitles) {

    public ReferenceData {
        countries = List.copyOf(countries);
        currencies = List.copyOf(currencies);
        departments = List.copyOf(departments);
        jobTitles = List.copyOf(jobTitles);
    }

    public Map<String, Currency> currenciesByCode() {
        return currencies.stream().collect(Collectors.toMap(Currency::code, Function.identity()));
    }

    public Currency currencyOfCountry(String countryCode) {
        Country country = findCountry(countryCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown country: " + countryCode));
        return currenciesByCode().get(country.currencyCode());
    }

    public Optional<Country> findCountry(String countryCode) {
        return countries.stream().filter(country -> country.code().equals(countryCode)).findFirst();
    }

    public Optional<Department> findDepartment(long departmentId) {
        return departments.stream().filter(department -> department.id() == departmentId).findFirst();
    }

    public Optional<JobTitle> findJobTitle(long jobTitleId) {
        return jobTitles.stream().filter(jobTitle -> jobTitle.id() == jobTitleId).findFirst();
    }
}
