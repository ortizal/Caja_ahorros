package com.alantek.caja.shared;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public static <T> PageResponse<T> ofList(List<T> items, int page, int size, Comparator<T> sort) {
        List<T> sorted = new ArrayList<>(items);
        if (sort != null) {
            sorted.sort(sort);
        }
        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        List<T> slice = from <= to ? List.copyOf(sorted.subList(from, to)) : List.of();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) sorted.size() / size);
        return new PageResponse<>(slice, page, size, sorted.size(), totalPages);
    }
}
