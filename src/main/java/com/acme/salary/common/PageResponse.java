package com.acme.salary.common;

import java.util.List;

/** One page of results together with the totals needed to render pagination controls. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = (int) ((totalItems + size - 1) / size);
        return new PageResponse<>(items, page, size, totalItems, totalPages);
    }
}
