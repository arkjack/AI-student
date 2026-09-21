package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.AiReplyVO;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek（OpenAI 兼容）AI 接入服务。
 *
 * <p>所有方法都经过统一的内容安全链路：输入侧检测 → 调用大模型 → 输出侧三重过滤 → 写交互日志。
 * 返回的 {@link AiReplyVO} 会带上 {@code filtered} 标记，前端据此区分「正常生成内容」
 * 与「被安全机制处置后的安全文案」。</p>
 */
public interface AiService {

    /**
     * 核心聊天方法：按当前 ai_config（密钥、模型、超时、限流、内容安全）调用大模型。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型回答；网络异常/超时/非 200 时降级返回兜底文案
     */
    AiReplyVO chat(String systemPrompt, String userPrompt);

    /**
     * AI 创意写作。
     *
     * @param topic  主题（会先做输入侧合规检测）
     * @param style  风格（小学童话/科幻等）
     * @param length 目标字数
     */
    AiReplyVO writing(String topic, String style, int length);

    /**
     * AI 智能答疑（科普助手）。
     *
     * @param question 学生提问（会先做输入侧合规检测）
     */
    AiReplyVO answer(String question);

    /**
     * 知识闯关出题（默认基础难度）。
     *
     * @param count 题目数量
     * @return 题目列表 [{question, options, answer}]
     */
    List<Map<String, Object>> genQuiz(int count);

    /**
     * 知识闯关出题。
     *
     * @param level 难度（1 基础 2 进阶 3 挑战）
     * @param count 题目数量
     * @return 题目列表 [{question, options, answer}]；解析或准确性校验失败时回退内置题库
     */
    List<Map<String, Object>> genQuiz(int level, int count);
}
