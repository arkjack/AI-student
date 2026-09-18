package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;
    private Long studentId;
    private String content;
    /** 1已提交 2已批改 */
    private Integer status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
