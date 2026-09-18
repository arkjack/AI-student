package com.edu.aienlighten.service;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek（OpenAI 兼容）AI 接入服务。
 */
public interface AiService {

    /**
     * 核心聊天方法：按当前 ai_config（密钥、模型、超时、限流、内容安全）调用大模型。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型回答；网络异常/超时/非 200 时降级返回兜底文案
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * AI 创意写作。
     *
     * @param topic  主题
     * @param style  风格（小学童话/科幻等）
     * @param length 目标字数
     * @return 作文正文
     */
    String writing(String topic, String style, int length);

    /**
     * AI 智能答疑（科普助手）。
     *
     * @param question 学生提问
     * @return 回答文本
     */
    String answer(String question);

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
     * @return 题目列表 [{question, options, answer}]
     */
    List<Map<String, Object>> genQuiz(int level, int count);
}
