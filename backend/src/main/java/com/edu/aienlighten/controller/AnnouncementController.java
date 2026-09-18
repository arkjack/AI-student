package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.entity.Announcement;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公告模块（学生端，任意登录角色可读） */
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /** 公告列表（置顶优先 + 时间倒序） */
    @GetMapping("/list")
    @RequireRole({0, 1, 2})
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.list());
    }
}
