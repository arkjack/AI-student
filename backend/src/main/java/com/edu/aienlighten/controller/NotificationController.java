package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 站内消息（学生/教师共用） */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 我的消息 */
    @GetMapping("/my")
    @RequireRole({0, 1, 2})
    public Result<List<NotificationVO>> my() {
        return Result.ok(notificationService.my());
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    @RequireRole({0, 1, 2})
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount());
    }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    @RequireRole({0, 1, 2})
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    /** 单条标记已读 */
    @PostMapping("/{id}/read")
    @RequireRole({0, 1, 2})
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }
}
