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

    /**
     * 来源任务 id（可空）。
     * 从作业中心带着 assignmentId 进来做任务时传，用于把作品与任务关联，
     * 老师批改后才能把分数回写到这份作品上；学生自由创作时不传。
     */
    private Long assignmentId;
}
