package com.acme.salary.util;

/** Helpers for building safe SQL fragments. */
public final class SqlUtils {

    public static final char LIKE_ESCAPE = '\\';

    private SqlUtils() {
    }

    /** Escapes LIKE wildcards so user input is matched literally; pair with {@code ESCAPE '\'}. */
    public static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** Builds a case-insensitive "starts with" LIKE pattern from raw user input. */
    public static String prefixPattern(String value) {
        return escapeLike(value.trim()) + "%";
    }
}
