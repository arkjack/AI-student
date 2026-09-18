package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.WritingSaveDTO;
import com.edu.aienlighten.entity.AiWriting;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.WritingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AI 创意写作模块（学生端） */
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    /** 我的写作记录 */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<List<AiWriting>> my() {
        return Result.ok(writingService.myWritings());
    }

    /** 保存草稿/提交写作 */
    @PostMapping("/save")
    @RequireRole({2})
    public Result<AiWriting> save(@Valid @RequestBody WritingSaveDTO dto) {
        return Result.ok(writingService.saveWriting(dto));
    }
}
