package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementDTO {

    @NotBlank(message = "公告标题不能为空")
    private String title;

    private String content;

    /** 公告/活动/维护 */
    private String tag;

    /** 1置顶 0普通 */
    private Integer isTop;
}
