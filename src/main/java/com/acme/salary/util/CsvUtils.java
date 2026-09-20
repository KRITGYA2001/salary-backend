package com.acme.salary.util;

import java.util.List;
import java.util.stream.Collectors;

/** RFC 4180 CSV escaping with protection against spreadsheet formula injection. */
public final class CsvUtils {

    public static final String LINE_SEPARATOR = "\r\n";

    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    private CsvUtils() {
    }

    /**
     * Escapes one text cell. Cells starting with a formula trigger are prefixed with an apostrophe so
     * spreadsheets show them as text; cells containing delimiters, quotes or line breaks are quoted.
     */
    public static String escapeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        boolean needsQuoting = safe.indexOf(DELIMITER) >= 0 || safe.indexOf(QUOTE) >= 0
                || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0;
        return needsQuoting ? QUOTE + safe.replace("\"", "\"\"") + QUOTE : safe;
    }

    /** Joins already-escaped cells into one line terminated by {@link #LINE_SEPARATOR}. */
    public static String toLine(List<String> escapedCells) {
        return escapedCells.stream().collect(Collectors.joining(String.valueOf(DELIMITER), "", LINE_SEPARATOR));
    }
}
