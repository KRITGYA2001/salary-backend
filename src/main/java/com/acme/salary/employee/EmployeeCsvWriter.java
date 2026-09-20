package com.acme.salary.employee;

import static com.acme.salary.util.CsvUtils.escapeText;

import com.acme.salary.util.CsvUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

/** Writes employees as CSV rows to a caller-owned writer; the writer is not closed. */
public class EmployeeCsvWriter {

    public static final String CONTENT_TYPE = "text/csv;charset=UTF-8";
    public static final String CONTENT_DISPOSITION = "attachment; filename=\"employees.csv\"";

    /** Lets Excel detect UTF-8 so non-ASCII names display correctly. */
    private static final String BYTE_ORDER_MARK = "﻿";

    private static final List<String> HEADER = List.of(
            "Employee Code", "Full Name", "Email", "Department", "Job Title", "Country", "Currency",
            "Employment Type", "Salary", "Salary (USD)", "Hire Date", "Status");

    private final Writer writer;

    public EmployeeCsvWriter(Writer writer) {
        this.writer = writer;
    }

    public void writeHeader() {
        write(BYTE_ORDER_MARK + CsvUtils.toLine(HEADER));
    }

    public void writeRow(EmployeeSummary employee) {
        write(CsvUtils.toLine(List.of(
                escapeText(employee.employeeCode()),
                escapeText(employee.fullName()),
                escapeText(employee.email()),
                escapeText(employee.department()),
                escapeText(employee.jobTitle()),
                escapeText(employee.countryName()),
                employee.currency(),
                employee.employmentType().name(),
                employee.salary().toPlainString(),
                employee.salaryUsd().toPlainString(),
                employee.hireDate().toString(),
                employee.status().name())));
    }

    private void write(String text) {
        try {
            writer.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
