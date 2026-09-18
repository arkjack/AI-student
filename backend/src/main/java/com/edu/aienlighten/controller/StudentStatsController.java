package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.StudentStatsService;
import com.edu.aienlighten.vo.StudentStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 学生学习数据（聚合自真实业务表） */
@RestController
@RequestMapping("/api/stats/student")
@RequiredArgsConstructor
public class StudentStatsController {

    private final StudentStatsService studentStatsService;

    /** 我的学习数据 */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<StudentStatsVO> my() {
        return Result.ok(studentStatsService.myStats());
    }
}
