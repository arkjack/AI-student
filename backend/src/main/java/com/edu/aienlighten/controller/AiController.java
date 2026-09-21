package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AiChatDTO;
import com.edu.aienlighten.dto.AiQuizDTO;
import com.edu.aienlighten.dto.AiWritingDTO;
import com.edu.aienlighten.entity.AiScenePolicy;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AiService;
import com.edu.aienlighten.service.ContentSafetyService;
import com.edu.aienlighten.vo.AiReplyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI 接入接口（学生/教师/管理员均可用）。
 *
 * <p>三个接口都走统一的内容安全链路。写作与答疑返回 {@link AiReplyVO}，
 * 其中 {@code filtered=true} 表示这次生成被安全机制处置过，前端应展示 {@code tip}
 * 而不是把安全文案当成正文。输入不合规时接口返回业务码 1201 且**不会调用大模型**。</p>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ContentSafetyService contentSafetyService;

    /** AI 创意写作 */
    @PostMapping("/writing")
    @RequireRole({0, 1, 2})
    public Result<AiReplyVO> writing(@Valid @RequestBody AiWritingDTO dto) {
        int length = dto.getLength() == null || dto.getLength() <= 0 ? 300 : dto.getLength();
        return Result.ok(aiService.writing(dto.getTopic(), dto.getStyle(), length));
    }

    /** AI 智能答疑 */
    @PostMapping("/chat")
    @RequireRole({0, 1, 2})
    public Result<AiReplyVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.ok(aiService.answer(dto.getQuestion()));
    }

    /** 知识闯关出题 */
    @PostMapping("/quiz")
    @RequireRole({0, 1, 2})
    public Result<List<Map<String, Object>>> quiz(@RequestBody AiQuizDTO dto) {
        int level = dto.getLevel() == null ? 1 : dto.getLevel();
        int count = dto.getCount() == null || dto.getCount() <= 0 ? 5 : dto.getCount();
        return Result.ok(aiService.genQuiz(level, count));
    }

    /**
     * 学生端读取某个实验的参数。
     *
     * <p>只返回学生真正用得到的展示参数（默认风格、每轮题量、答题限时），
     * <b>不返回任何安全开关</b>——安全策略是管理端的配置，学生端无需也不应知道。</p>
     */
    @GetMapping("/scene-policy/{scene}")
    @RequireRole({0, 1, 2})
    public Result<Map<String, Object>> scenePolicy(@PathVariable String scene) {
        AiScenePolicy p = contentSafetyService.policyOf(scene);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scene", p.getScene());
        data.put("defaultStyle", p.getDefaultStyle());
        data.put("genCount", p.getGenCount());
        data.put("answerLimitSec", p.getAnswerLimitSec());
        return Result.ok(data);
    }
}
