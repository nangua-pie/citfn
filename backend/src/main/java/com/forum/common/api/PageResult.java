package com.forum.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transport friendly page wrapper, decoupled from MyBatis-Plus {@code IPage}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    // Counts and page numbers are small, so they stay JSON numbers; only
    // identifiers need string form to survive a JavaScript client.
    @JsonSerialize(using = LongAsNumberSerializer.class)
    private long total;
    @JsonSerialize(using = LongAsNumberSerializer.class)
    private long page;
    @JsonSerialize(using = LongAsNumberSerializer.class)
    private long size;
    @JsonSerialize(using = LongAsNumberSerializer.class)
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(records == null ? new ArrayList<>() : records, total, page, size, pages);
    }

    public static <E, T> PageResult<T> from(IPage<E> page, Function<E, T> mapper) {
        List<T> mapped = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return of(mapped, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
