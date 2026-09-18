package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ProgressSaveDTO;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.ProgressService;
import com.edu.aienlighten.vo.CourseProgressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 学习进度模块（学生端） */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /** 我的学习进度 */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<List<CourseProgressVO>> my() {
        return Result.ok(progressService.myProgress());
    }

    /** 更新/保存学习进度（progress=100 时自动标记完成） */
    @PostMapping("/save")
    @RequireRole({2})
    public Result<CourseProgress> save(@Valid @RequestBody ProgressSaveDTO dto) {
        return Result.ok(progressService.saveProgress(dto));
    }
}
