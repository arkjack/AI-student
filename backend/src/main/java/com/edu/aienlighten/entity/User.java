package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /**
     * BCrypt 密码散列。
     * ⚠️ 必须 @JsonIgnore：/api/user/me 直接返回本实体，不加注解会把散列下发给前端。
     */
    @JsonIgnore
    private String password;

    private String realName;
    private String nickname;
    private String avatar;
    /** 0管理员 1教师 2学生 */
    private Integer role;
    /** 教师学科 */
    private String subject;
    /** 1启用 0禁用 */
    private Integer status;
    /** 1接收站内消息 0不接收（关闭后后端不再为该用户生成站内消息） */
    private Integer notifyEnabled;
    /** 1开启学习提醒 0关闭（开启后长期未学习会收到提醒消息） */
    private Integer remindEnabled;
    /** 密码最后修改时间：用于作废此前签发的 token */
    private LocalDateTime passwordChangedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
