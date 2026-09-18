package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_record")
public class QaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String question;
    /** 回答（AI 或教师） */
    private String answer;
    /** 1AI 2教师 */
    private Integer answerType;
    private Long teacherId;
    private LocalDateTime createdAt;
}
