package com.acme.salary.employee;

/** SQL fragments shared by employee queries. */
public final class EmployeeSql {

    /** Annual salary converted to US dollars using the currency's minor-unit exponent and rate. */
    public static final String SALARY_USD = """
            (e.salary_minor * cur.usd_per_unit
                / CASE cur.minor_unit_exponent WHEN 0 THEN 1.0 WHEN 1 THEN 10.0 WHEN 2 THEN 100.0 ELSE 1000.0 END)""";

    public static final String FROM_EMPLOYEE_JOINS = """
            FROM employee e
            JOIN department d ON d.id = e.department_id
            JOIN job_title jt ON jt.id = e.job_title_id
            JOIN country co ON co.code = e.country_code
            JOIN currency cur ON cur.code = co.currency""";

    private EmployeeSql() {
    }
}
