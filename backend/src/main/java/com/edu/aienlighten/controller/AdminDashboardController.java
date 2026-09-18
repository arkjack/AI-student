package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminDashboardService;
import com.edu.aienlighten.vo.DashboardSummaryVO;
import com.edu.aienlighten.vo.RegisterTrendVO;
import com.edu.aienlighten.vo.RoleDistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端 · 看板统计 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /** 汇总 */
    @GetMapping("/summary")
    @RequireRole({0})
    public Result<DashboardSummaryVO> summary() {
        return Result.ok(adminDashboardService.summary());
    }

    /** 近 12 周注册趋势 */
    @GetMapping("/register-trend")
    @RequireRole({0})
    public Result<List<RegisterTrendVO>> registerTrend() {
        return Result.ok(adminDashboardService.registerTrend());
    }

    /** 角色分布 */
    @GetMapping("/role-dist")
    @RequireRole({0})
    public Result<List<RoleDistVO>> roleDist() {
        return Result.ok(adminDashboardService.roleDist());
    }
}
