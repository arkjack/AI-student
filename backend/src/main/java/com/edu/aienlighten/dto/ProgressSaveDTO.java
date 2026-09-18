package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgressSaveDTO {

    @NotNull(message = "课程 id 不能为空")
    private Long courseId;

    /** 0-100 */
    private Integer progress;

    private Integer watchSeconds;
}
