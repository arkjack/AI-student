package com.edu.aienlighten.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseProgressVO {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private String coverEmoji;
    private Integer progress;
    private Integer watchSeconds;
    private Integer completed;
    private LocalDateTime updatedAt;
}
