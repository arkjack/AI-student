package com.edu.aienlighten.vo;

import lombok.Data;

/** 注册趋势 VO（近 12 周） */
@Data
public class RegisterTrendVO {

    /** 周起始日期 yyyy-MM-dd */
    private String label;
    private Long count;
}
