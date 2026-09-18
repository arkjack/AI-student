package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.QuizRecordDTO;
import com.edu.aienlighten.entity.QuizQuestion;
import com.edu.aienlighten.entity.QuizRecord;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 闯关模块（学生端） */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** 按关卡返回题目（默认 AI 基础） */
    @GetMapping("/questions")
    @RequireRole({2})
    public Result<List<QuizQuestion>> questions(@RequestParam(required = false) String level) {
        return Result.ok(quizService.questions(level));
    }

    /** 保存闯关成绩 */
    @PostMapping("/record")
    @RequireRole({2})
    public Result<QuizRecord> record(@Valid @RequestBody QuizRecordDTO dto) {
        return Result.ok(quizService.saveRecord(dto));
    }

    /** 关卡列表（去重） */
    @GetMapping("/levels")
    @RequireRole({2})
    public Result<List<String>> levels() {
        return Result.ok(quizService.levels());
    }

    /** 我的历次闯关成绩（倒序） */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<List<QuizRecord>> my() {
        return Result.ok(quizService.myRecords());
    }
}
