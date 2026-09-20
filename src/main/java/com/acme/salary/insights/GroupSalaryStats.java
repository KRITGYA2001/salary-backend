package com.acme.salary.insights;

/** Statistics for one group, for example one country; {@code key} is stable, {@code label} is for display. */
public record GroupSalaryStats(String key, String label, SalaryStats stats) {
}
