package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private Long categoryId;
    /** 1入门 2进阶 3挑战 */
    private Integer difficulty;
    private Integer durationMinutes;
    private String coverEmoji;
    private String coverImage;
    private String summary;
    private String videoUrl;
    private String teacher;
    private Integer views;
    /** 1上架 0下架 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
