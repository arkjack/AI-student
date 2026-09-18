package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息通知 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户 id */
    private Long userId;
    /** task作业 grade批改 announce公告 apply入班 */
    private String type;
    private String title;
    private String content;
    /** 点击跳转的前端路由 */
    private String link;
    /** 0未读 1已读 */
    private Integer isRead;
    private LocalDateTime createdAt;
}
