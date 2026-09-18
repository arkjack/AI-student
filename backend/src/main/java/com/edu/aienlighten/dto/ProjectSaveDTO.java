package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectSaveDTO {

    @NotBlank(message = "作品标题不能为空")
    private String title;

    private String blocksJson;

    private String codeText;

    /** 0草稿 1已提交 */
    private Integer status;
}
