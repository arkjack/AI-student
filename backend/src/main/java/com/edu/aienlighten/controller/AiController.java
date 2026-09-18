package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AiChatDTO;
import com.edu.aienlighten.dto.AiQuizDTO;
import com.edu.aienlighten.dto.AiWritingDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** DeepSeek AI 接入接口（学生/教师/管理员均可用） */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** AI 创意写作 */
    @PostMapping("/writing")
    @RequireRole({0, 1, 2})
    public Result<Map<String, String>> writing(@Valid @RequestBody AiWritingDTO dto) {
        int length = dto.getLength() == null || dto.getLength() <= 0 ? 300 : dto.getLength();
        String content = aiService.writing(dto.getTopic(), dto.getStyle(), length);
        return Result.ok(Map.of("content", content));
    }

    /** AI 智能答疑 */
    @PostMapping("/chat")
    @RequireRole({0, 1, 2})
    public Result<Map<String, String>> chat(@Valid @RequestBody AiChatDTO dto) {
        String answer = aiService.answer(dto.getQuestion());
        return Result.ok(Map.of("answer", answer));
    }

    /** 知识闯关出题 */
    @PostMapping("/quiz")
    @RequireRole({0, 1, 2})
    public Result<List<Map<String, Object>>> quiz(@RequestBody AiQuizDTO dto) {
        int level = dto.getLevel() == null ? 1 : dto.getLevel();
        int count = dto.getCount() == null || dto.getCount() <= 0 ? 5 : dto.getCount();
        return Result.ok(aiService.genQuiz(level, count));
    }
}
