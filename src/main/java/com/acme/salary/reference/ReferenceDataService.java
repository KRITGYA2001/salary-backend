package com.acme.salary.reference;

import org.springframework.stereotype.Service;

@Service
public class ReferenceDataService {

    private final ReferenceDataRepository repository;

    public ReferenceDataService(ReferenceDataRepository repository) {
        this.repository = repository;
    }

    /** Loads a fresh snapshot; the tables are tiny, so no caching is needed. */
    public ReferenceData load() {
        return new ReferenceData(
                repository.findAllCountries(),
                repository.findAllCurrencies(),
                repository.findAllDepartments(),
                repository.findAllJobTitles());
    }
}
