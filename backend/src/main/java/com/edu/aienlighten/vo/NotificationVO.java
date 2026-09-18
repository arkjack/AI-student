package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息 VO */
@Data
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String link;
    private Boolean read;
    private LocalDateTime createdAt;
}
