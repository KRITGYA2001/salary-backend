package com.acme.salary.seed;

import static com.acme.salary.seed.SeedConstants.DEFAULT_EMPLOYEE_COUNT;
import static com.acme.salary.seed.SeedConstants.RANDOM_SEED;

import com.acme.salary.employee.NewEmployee;
import com.acme.salary.reference.ReferenceData;
import com.acme.salary.reference.ReferenceDataService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fills an empty database with the deterministic demo data set on startup, when enabled. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final SeedRepository seedRepository;
    private final ReferenceDataService referenceDataService;
    private final Clock clock;
    private final int employeeCount;

    public SeedRunner(
            SeedRepository seedRepository,
            ReferenceDataService referenceDataService,
            Clock clock,
            @Value("${app.seed.count:" + DEFAULT_EMPLOYEE_COUNT + "}") int employeeCount) {
        this.seedRepository = seedRepository;
        this.referenceDataService = referenceDataService;
        this.clock = clock;
        this.employeeCount = employeeCount;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedRepository.hasEmployees()) {
            log.info("Employees already present, skipping seed");
            return;
        }
        long startedAtNanos = System.nanoTime();
        ReferenceData referenceData = referenceDataService.load();
        EmployeeGenerator generator = new EmployeeGenerator(referenceData, RANDOM_SEED, LocalDate.now(clock));

        List<NewEmployee> employees = new ArrayList<>(employeeCount);
        for (int sequenceNumber = 1; sequenceNumber <= employeeCount; sequenceNumber++) {
            employees.add(generator.next(sequenceNumber));
        }
        seedRepository.insertAll(employees, Instant.now(clock));
        log.info("Seeded {} employees in {} ms", employeeCount, (System.nanoTime() - startedAtNanos) / 1_000_000);
    }
}
