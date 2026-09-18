package com.edu.aienlighten.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private long total;
    private List<T> records;
    private long page;
    private long size;

    public static <T> PageVO<T> of(Page<T> p) {
        PageVO<T> vo = new PageVO<>();
        vo.total = p.getTotal();
        vo.records = p.getRecords();
        vo.page = p.getCurrent();
        vo.size = p.getSize();
        return vo;
    }
}
