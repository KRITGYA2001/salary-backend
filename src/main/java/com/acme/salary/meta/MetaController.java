package com.acme.salary.meta;

import com.acme.salary.common.ApiPaths;
import com.acme.salary.reference.ReferenceData;
import com.acme.salary.reference.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.META)
public class MetaController {

    private final ReferenceDataService referenceDataService;

    public MetaController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    /** Lists countries (with their currency), departments and job titles (with their department). */
    @GetMapping("/filters")
    public FilterOptions filters() {
        ReferenceData referenceData = referenceDataService.load();
        return new FilterOptions(referenceData.countries(), referenceData.departments(), referenceData.jobTitles());
    }
}
