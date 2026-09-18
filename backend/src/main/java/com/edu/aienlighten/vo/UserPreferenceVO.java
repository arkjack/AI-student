package com.edu.aienlighten.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户偏好设置（个人中心「设置」区） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceVO {

    /** 是否接收站内消息 */
    private Boolean notifyEnabled;
    /** 是否开启学习提醒 */
    private Boolean remindEnabled;
}
