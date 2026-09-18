package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.vo.ExpLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 经验明细：只读，且只返回当前登录用户自己的流水 */
@RestController
@RequestMapping("/api/exp")
@RequiredArgsConstructor
public class ExpController {

    private final ExpService expService;

    /**
     * 我的经验明细。
     * 只按 {@code UserContext.userId()} 查询，不接受前端传 studentId ——
     * 否则就成了水平越权（可以看别人赚了多少经验）。
     */
    @GetMapping("/log")
    @RequireRole({0, 1, 2})
    public Result<List<ExpLogVO>> myLog(@RequestParam(required = false, defaultValue = "20") Integer limit) {
        return Result.ok(expService.recentLogs(UserContext.userId(), limit == null ? 20 : limit));
    }
}
