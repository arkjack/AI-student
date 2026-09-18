package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WritingSaveDTO {

    @NotBlank(message = "主题不能为空")
    private String topic;

    private String style;

    private String content;

    /** 0草稿 1已提交 */
    private Integer status;
}
