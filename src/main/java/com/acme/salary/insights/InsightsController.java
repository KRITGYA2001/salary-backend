package com.acme.salary.insights;

import static com.acme.salary.insights.InsightsConstants.DEFAULT_BUCKET_COUNT;
import static com.acme.salary.insights.InsightsConstants.DEFAULT_TOP_LIMIT;
import static com.acme.salary.insights.InsightsConstants.MAX_BUCKET_COUNT;
import static com.acme.salary.insights.InsightsConstants.MAX_TOP_LIMIT;

import com.acme.salary.common.ApiPaths;
import com.acme.salary.employee.EmployeeSummary;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only pay analytics over active employees, with every amount normalised to US dollars. */
@RestController
@RequestMapping(ApiPaths.INSIGHTS)
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/summary")
    public SalaryStats summary(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long jobTitle) {
        return insightsService.summarize(toFilter(country, department, jobTitle));
    }

    @GetMapping("/by/{dimension}")
    public List<GroupSalaryStats> byDimension(
            @PathVariable String dimension,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long jobTitle) {
        return insightsService.groupBy(
                InsightDimension.fromParameter(dimension), toFilter(country, department, jobTitle));
    }

    @GetMapping("/distribution")
    public List<SalaryBucket> distribution(
            @RequestParam(defaultValue = "" + DEFAULT_BUCKET_COUNT) int buckets,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long jobTitle) {
        requireWithin("buckets", buckets, MAX_BUCKET_COUNT);
        return insightsService.distribution(toFilter(country, department, jobTitle), buckets);
    }

    @GetMapping("/top")
    public List<EmployeeSummary> top(
            @RequestParam(defaultValue = "highest") String direction,
            @RequestParam(defaultValue = "" + DEFAULT_TOP_LIMIT) int limit,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long jobTitle) {
        requireWithin("limit", limit, MAX_TOP_LIMIT);
        return insightsService.top(
                toFilter(country, department, jobTitle), TopDirection.fromParameter(direction), limit);
    }

    private static InsightsFilter toFilter(String country, Long department, Long jobTitle) {
        return new InsightsFilter(
                StringUtils.hasText(country) ? country.toUpperCase() : null, department, jobTitle);
    }

    private static void requireWithin(String name, int value, int max) {
        if (value < 1 || value > max) {
            throw new IllegalArgumentException(name + " must be between 1 and " + max);
        }
    }
}
