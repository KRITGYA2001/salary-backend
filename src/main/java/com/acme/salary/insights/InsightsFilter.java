package com.acme.salary.insights;

/** Optional restrictions applied to every insight; null means "all". */
public record InsightsFilter(String countryCode, Long departmentId, Long jobTitleId) {
}
