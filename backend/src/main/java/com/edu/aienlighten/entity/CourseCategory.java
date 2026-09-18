package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_category")
public class CourseCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String emoji;
    private Integer sort;
}
