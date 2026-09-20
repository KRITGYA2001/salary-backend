package com.acme.salary.reference;

import java.util.List;
import java.util.Map;
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
        String currencyCode = countries.stream()
                .filter(country -> country.code().equals(countryCode))
                .map(Country::currencyCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown country: " + countryCode));
        return currenciesByCode().get(currencyCode);
    }
}
