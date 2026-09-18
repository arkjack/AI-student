package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_student")
public class ClassStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long studentId;
    private LocalDateTime joinedAt;
}
