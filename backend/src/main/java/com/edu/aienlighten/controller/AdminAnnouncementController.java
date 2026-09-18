package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AnnouncementDTO;
import com.edu.aienlighten.entity.Announcement;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端 · 公告管理 */
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    /** 公告列表 */
    @GetMapping
    @RequireRole({0})
    public Result<List<Announcement>> list() {
        return Result.ok(adminAnnouncementService.list());
    }

    /** 发布公告 */
    @PostMapping
    @RequireRole({0})
    public Result<Void> create(@Valid @RequestBody AnnouncementDTO dto) {
        adminAnnouncementService.create(dto);
        return Result.ok();
    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    @RequireRole({0})
    public Result<Void> delete(@PathVariable Long id) {
        adminAnnouncementService.delete(id);
        return Result.ok();
    }
}
