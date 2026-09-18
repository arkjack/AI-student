package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_progress")
public class CourseProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;
    /** 0-100 */
    private Integer progress;
    private Integer watchSeconds;
    /** 0 未完成 1 已完成 */
    private Integer completed;
    private LocalDateTime updatedAt;
}
