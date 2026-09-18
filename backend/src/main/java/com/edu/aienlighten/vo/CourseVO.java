package com.edu.aienlighten.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVO {

    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private Integer difficulty;
    private Integer durationMinutes;
    private String coverEmoji;
    private String coverImage;
    private String summary;
    private String videoUrl;
    private String teacher;
    private Integer views;
    private Integer status;
    private LocalDateTime createdAt;
    /** 当前用户学习进度 0-100 */
    private Integer progress;
    /** 是否已完成 1/0 */
    private Integer completed;
}
