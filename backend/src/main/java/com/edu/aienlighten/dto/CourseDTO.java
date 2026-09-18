package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseDTO {

    @NotBlank(message = "课程标题不能为空")
    private String title;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 1入门 2进阶 3挑战 */
    private Integer difficulty;

    private Integer durationMinutes;

    private String coverEmoji;
    private String coverImage;
    private String summary;
    private String videoUrl;

    /** 1上架 0下架 */
    private Integer status;
}
