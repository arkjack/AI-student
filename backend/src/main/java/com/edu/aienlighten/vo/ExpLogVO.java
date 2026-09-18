package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 经验流水条目（给学生看的「经验明细」） */
@Data
public class ExpLogVO {

    private Long id;
    private Integer exp;
    private String sourceType;
    private String remark;
    private LocalDateTime createdAt;
}
