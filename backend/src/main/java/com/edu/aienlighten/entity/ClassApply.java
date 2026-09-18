package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 入班申请 */
@Data
@TableName("class_apply")
public class ClassApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 user.id */
    private Long studentId;

    /** 申请的班级 id */
    private Long classId;

    /** 0待审 1同意 2拒绝 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
