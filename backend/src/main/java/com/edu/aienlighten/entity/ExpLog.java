package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 经验流水。总经验 = 同一 student_id 下所有 exp 之和。 */
@Data
@TableName("exp_log")
public class ExpLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** daily/course/project/writing/quiz/submission/streak/backfill */
    private String sourceType;

    /** 幂等键：(student_id, source_key) 唯一，同一件事只记一次 */
    private String sourceKey;

    /** 本次经验，正数为获得，负数为撤销 */
    private Integer exp;

    private String remark;

    private LocalDateTime createdAt;
}
