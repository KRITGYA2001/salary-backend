package com.acme.salary.meta;

import com.acme.salary.reference.Country;
import com.acme.salary.reference.Department;
import com.acme.salary.reference.JobTitle;
import java.util.List;

/** Values that populate the filter and form dropdowns of the UI. */
public record FilterOptions(List<Country> countries, List<Department> departments, List<JobTitle> jobTitles) {
}
