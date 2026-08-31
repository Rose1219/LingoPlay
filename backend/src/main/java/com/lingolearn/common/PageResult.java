package com.lingolearn.common;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/** 分页结果 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(Page<T> p) {
        PageResult<T> r = new PageResult<>();
        r.records = p.getContent();
        r.total = p.getTotalElements();
        r.page = p.getNumber() + 1;
        r.size = p.getSize();
        return r;
    }
}