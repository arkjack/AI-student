package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
