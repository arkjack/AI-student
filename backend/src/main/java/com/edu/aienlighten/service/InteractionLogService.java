package com.edu.aienlighten.service;

import com.edu.aienlighten.common.SafetyResult;
import com.edu.aienlighten.dto.ContentReviewDTO;
import com.edu.aienlighten.vo.AiInteractionLogVO;
import com.edu.aienlighten.vo.PageVO;

import java.util.Map;

/**
 * AI 交互日志与人工复核。
 *
 * <p>任务书要求「所有交互日志留存可追溯」并「支持教师后台查看与人工复核」，
 * 这两件事落在同一张 {@code ai_interaction_log} 上：写入用 {@link #record}，
 * 查看看 {@link #pageForAdmin} / {@link #pageForTeacher}，复核走 {@link #review}。</p>
 */
public interface InteractionLogService {

    /** 一次 AI 调用的上下文（不含安全结论，安全结论单独由 SafetyResult 传入） */
    record AiCall(String scene, String inputText, String outputText, String model, Integer elapsedMs) {
    }

    /**
     * 落一条交互日志。
     *
     * <p><b>本方法绝不抛异常</b>：写日志失败只能降级为一条 warn 日志，
     * 不能反过来把正常的 AI 功能搞挂。</p>
     */
    void record(AiCall call, SafetyResult result);

    /** 输入被拦截：大模型没有被调用，因而没有 outputText */
    void recordInputBlock(String scene, String inputText, SafetyResult result);

    /** 网络异常或上游配置错误导致的降级：记录一次 fallback，便于统计 AI 可用性 */
    void recordFallback(AiCall call, String reason);

    /** 管理端：全量日志分页查询 */
    PageVO<AiInteractionLogVO> pageForAdmin(String scene, Integer hitStage, Integer reviewStatus,
                                            String keyword, String startDate, String endDate,
                                            Integer page, Integer size);

    /** 教师端：仅本班学生的记录 */
    PageVO<AiInteractionLogVO> pageForTeacher(Integer reviewStatus, String scene,
                                              String keyword, Integer page, Integer size);

    /** 日志详情；教师只能看本班学生，管理员不限 */
    AiInteractionLogVO detail(Long id, boolean admin);

    /** 人工复核；教师只能复核本班学生的记录，管理员不限 */
    void review(Long id, ContentReviewDTO dto, boolean admin);

    /** 教师端待复核条数（用于菜单红点） */
    long pendingCountForTeacher();

    /** 管理端待复核条数 */
    long pendingCountForAdmin();

    /**
     * 管理端内容安全概览：近 N 天的调用量、被拦截量、被替换量、降级量，以及当前待复核量。
     */
    Map<String, Object> summary(int days);
}
