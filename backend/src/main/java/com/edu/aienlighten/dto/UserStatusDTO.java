package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {

    @NotNull(message = "状态不能为空")
    /** 1启用 0禁用 */
    private Integer status;
}
