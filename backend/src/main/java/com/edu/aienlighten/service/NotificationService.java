package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.NotificationVO;

import java.util.List;

public interface NotificationService {

    /** 给某个用户推送一条站内消息 */
    void push(Long userId, String type, String title, String content, String link);

    /** 我的消息（倒序） */
    List<NotificationVO> my();

    /** 未读数 */
    long unreadCount();

    /** 全部标记已读 */
    void markAllRead();

    /** 单条标记已读 */
    void markRead(Long id);
}
