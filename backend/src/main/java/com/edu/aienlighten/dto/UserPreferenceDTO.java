package com.edu.aienlighten.dto;

import lombok.Data;

/** 更新用户偏好设置请求（字段为 null 表示不修改该项） */
@Data
public class UserPreferenceDTO {

    /** 是否接收站内消息 */
    private Boolean notifyEnabled;
    /** 是否开启学习提醒 */
    private Boolean remindEnabled;
}
