package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.QaAskDTO;
import com.edu.aienlighten.entity.QaRecord;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.QaService;
import com.edu.aienlighten.vo.QaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 答疑模块（学生端） */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    /** 提问（AI 回答预留给 P5） */
    @PostMapping("/ask")
    @RequireRole({2})
    public Result<QaRecord> ask(@Valid @RequestBody QaAskDTO dto) {
        return Result.ok(qaService.ask(dto));
    }

    /** 我的问答历史（近 50 条） */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<List<QaVO>> my() {
        return Result.ok(qaService.myQuestions());
    }
}
