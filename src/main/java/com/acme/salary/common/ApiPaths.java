package com.acme.salary.common;

/** URL path fragments shared by controllers. */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";
    public static final String EMPLOYEES = API_V1 + "/employees";
    public static final String INSIGHTS = API_V1 + "/insights";

    private ApiPaths() {
    }
}
