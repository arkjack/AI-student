package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.entity.OperationLog;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/** 管理端 · 操作日志 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogService operationLogService;

    /** 分页查询操作日志 */
    @GetMapping
    @RequireRole({0})
    public Result<PageVO<OperationLog>> page(@RequestParam(required = false) Integer operator,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        Page<OperationLog> p = operationLogService.page(operator, page, size);
        return Result.ok(PageVO.of(p));
    }
}
