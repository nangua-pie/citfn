package com.forum.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
    private long total;
    private long page;
    private long size;
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
