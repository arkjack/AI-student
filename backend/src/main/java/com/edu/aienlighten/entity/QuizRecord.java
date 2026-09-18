package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_record")
public class QuizRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String level;
    private Integer score;
    private Integer correctCount;
    private Integer totalCount;
    private LocalDateTime finishedAt;
}
