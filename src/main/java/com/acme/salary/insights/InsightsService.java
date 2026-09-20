package com.acme.salary.insights;

import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmployeeSearchCriteria;
import com.acme.salary.employee.EmployeeSortField;
import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.EmployeeSummary;
import com.acme.salary.insights.InsightsRepository.SalaryRange;
import com.acme.salary.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsightsService {

    private final InsightsRepository insightsRepository;
    private final EmployeeRepository employeeRepository;

    public InsightsService(InsightsRepository insightsRepository, EmployeeRepository employeeRepository) {
        this.insightsRepository = insightsRepository;
        this.employeeRepository = employeeRepository;
    }

    public SalaryStats summarize(InsightsFilter filter) {
        return insightsRepository.summarize(filter);
    }

    public List<GroupSalaryStats> groupBy(InsightDimension dimension, InsightsFilter filter) {
        return insightsRepository.groupBy(dimension, filter);
    }

    /** Builds an equal-width histogram over the filtered salaries; empty when nobody matches. */
    public List<SalaryBucket> distribution(InsightsFilter filter, int bucketCount) {
        SalaryRange range = insightsRepository.findRange(filter);
        if (range.headcount() == 0) {
            return List.of();
        }
        if (range.minUsd() == range.maxUsd()) {
            BigDecimal value = MoneyUtils.roundUsd(range.minUsd());
            return List.of(new SalaryBucket(value, value, range.headcount()));
        }

        Map<Integer, Long> countsByIndex =
                insightsRepository.countByBucket(filter, range.minUsd(), range.maxUsd(), bucketCount);
        double bucketWidth = (range.maxUsd() - range.minUsd()) / bucketCount;
        List<SalaryBucket> buckets = new ArrayList<>(bucketCount);
        for (int index = 0; index < bucketCount; index++) {
            double lower = range.minUsd() + index * bucketWidth;
            double upper = index == bucketCount - 1 ? range.maxUsd() : lower + bucketWidth;
            buckets.add(new SalaryBucket(
                    MoneyUtils.roundUsd(lower), MoneyUtils.roundUsd(upper), countsByIndex.getOrDefault(index, 0L)));
        }
        return buckets;
    }

    /** Returns the best or worst paid active employees, ranked by USD-normalised salary. */
    public List<EmployeeSummary> top(InsightsFilter filter, TopDirection direction, int limit) {
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                filter.countryCode(),
                filter.departmentId(),
                filter.jobTitleId(),
                EmployeeStatus.ACTIVE,
                null,
                EmployeeSortField.SALARY,
                direction.sortDirection(),
                0,
                limit);
        return employeeRepository.search(criteria);
    }
}
