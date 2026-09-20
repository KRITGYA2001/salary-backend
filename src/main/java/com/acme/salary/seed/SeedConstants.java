package com.acme.salary.seed;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Fixed inputs of the demo-data generator, kept together so the generated data set is reproducible. */
public final class SeedConstants {

    public static final long RANDOM_SEED = 20260101L;
    public static final int DEFAULT_EMPLOYEE_COUNT = 10_000;
    public static final int INSERT_BATCH_SIZE = 1_000;
    public static final String EMAIL_DOMAIN = "acme.example";
    public static final String EMPLOYEE_CODE_FORMAT = "EMP-%05d";
    public static final String INITIAL_SALARY_REASON = "Initial salary";
    public static final LocalDate EARLIEST_HIRE_DATE = LocalDate.of(2012, 1, 1);
    public static final int LATEST_HIRE_OFFSET_DAYS = 30;
    public static final int ACTIVE_PERCENTAGE = 95;
    public static final int FULL_TIME_PERCENTAGE = 85;
    public static final int PART_TIME_PERCENTAGE = 8;
    public static final double MIN_SALARY_FACTOR = 0.6;
    public static final double MAX_SALARY_FACTOR = 1.8;

    /** Typical annual salary in major units of each country's own currency. */
    public static final Map<String, Long> BASE_SALARY_BY_COUNTRY = Map.of(
            "US", 95_000L,
            "GB", 60_000L,
            "DE", 65_000L,
            "FR", 55_000L,
            "IN", 1_800_000L,
            "JP", 7_000_000L,
            "CA", 85_000L,
            "AU", 95_000L,
            "SG", 90_000L,
            "BR", 150_000L);

    public static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Olivia", "Liam", "Sofia", "Noah", "Yuki", "Emma", "Mateo", "Priya", "Lucas",
            "Hannah", "Kenji", "Amelia", "Rohan", "Chloe", "Ethan", "Mei", "Oliver", "Isabella", "Arjun",
            "Zoe", "Daniel", "Aisha", "Felix", "Nora", "Carlos", "Lena", "Ravi", "Grace", "Tomas");

    public static final List<String> LAST_NAMES = List.of(
            "Sharma", "Smith", "Tanaka", "Garcia", "Muller", "Patel", "Jones", "Silva", "Martin", "Kumar",
            "Brown", "Sato", "Lopez", "Schmidt", "Singh", "Taylor", "Costa", "Dubois", "Nair", "Wilson",
            "Chen", "Ferrari", "Reddy", "Clark", "Ito", "Santos", "Novak", "Gupta", "Walker", "Lim");

    private SeedConstants() {
    }
}
