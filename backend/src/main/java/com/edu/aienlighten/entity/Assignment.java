package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;
    private Long classId;
    private String title;
    private String content;
    /** 1课程 2编程 3AI实验 4闯关 */
    private Integer type;
    private Long resourceId;
    private LocalDateTime deadline;
    /** 1进行中 0已截止 */
    private Integer status;
    private LocalDateTime createdAt;
}
