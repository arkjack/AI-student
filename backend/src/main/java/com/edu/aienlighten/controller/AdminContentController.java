package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ContentReviewDTO;
import com.edu.aienlighten.dto.SensitiveWordDTO;
import com.edu.aienlighten.dto.SensitiveWordImportDTO;
import com.edu.aienlighten.dto.TemplateEnabledDTO;
import com.edu.aienlighten.entity.SensitiveWord;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.ContentWordService;
import com.edu.aienlighten.service.InteractionLogService;
import com.edu.aienlighten.vo.AiInteractionLogVO;
import com.edu.aienlighten.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 · 内容安全（敏感词库 + AI 交互日志 + 复核兜底）。
 *
 * <p>这是管理端「内容安全」页面的接口层：词库可增删改查与批量导入，
 * 每一次 AI 调用都能在日志里查到，并且管理员可以对教师未处理的记录做兜底复核。</p>
 */
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentWordService contentWordService;
    private final InteractionLogService interactionLogService;

    // ==================== 敏感词库 ====================

    /** 词库分页查询 */
    @GetMapping("/words")
    @RequireRole({0})
    public Result<PageVO<SensitiveWord>> words(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer category,
                                               @RequestParam(required = false) Integer enabled,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return Result.ok(contentWordService.page(keyword, category, enabled, page, size));
    }

    /** 新增敏感词 */
    @PostMapping("/words")
    @RequireRole({0})
    public Result<SensitiveWord> createWord(@Valid @RequestBody SensitiveWordDTO dto) {
        return Result.ok(contentWordService.create(dto));
    }

    /** 编辑敏感词 */
    @PutMapping("/words/{id}")
    @RequireRole({0})
    public Result<SensitiveWord> updateWord(@PathVariable Long id,
                                            @Valid @RequestBody SensitiveWordDTO dto) {
        return Result.ok(contentWordService.update(id, dto));
    }

    /** 删除敏感词 */
    @DeleteMapping("/words/{id}")
    @RequireRole({0})
    public Result<Void> deleteWord(@PathVariable Long id) {
        contentWordService.delete(id);
        return Result.ok();
    }

    /** 启停敏感词 */
    @PutMapping("/words/{id}/enabled")
    @RequireRole({0})
    public Result<Void> setWordEnabled(@PathVariable Long id, @RequestBody TemplateEnabledDTO dto) {
        contentWordService.setEnabled(id, Boolean.TRUE.equals(dto.getEnabled()));
        return Result.ok();
    }

    /** 批量导入：一行一个词 */
    @PostMapping("/words/import")
    @RequireRole({0})
    public Result<Map<String, Object>> importWords(@Valid @RequestBody SensitiveWordImportDTO dto) {
        int added = contentWordService.importWords(dto);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("added", added);
        return Result.ok(data);
    }

    /** 词库分类统计 */
    @GetMapping("/words/stats")
    @RequireRole({0})
    public Result<List<Map<String, Object>>> wordStats() {
        return Result.ok(contentWordService.categoryStats());
    }

    // ==================== AI 交互日志与复核 ====================

    /** 全量交互日志 */
    @GetMapping("/logs")
    @RequireRole({0})
    public Result<PageVO<AiInteractionLogVO>> logs(@RequestParam(required = false) String scene,
                                                   @RequestParam(required = false) Integer hitStage,
                                                   @RequestParam(required = false) Integer reviewStatus,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        return Result.ok(interactionLogService.pageForAdmin(
                scene, hitStage, reviewStatus, keyword, startDate, endDate, page, size));
    }

    /** 日志详情 */
    @GetMapping("/logs/{id}")
    @RequireRole({0})
    public Result<AiInteractionLogVO> logDetail(@PathVariable Long id) {
        return Result.ok(interactionLogService.detail(id, true));
    }

    /** 管理员兜底复核 */
    @PostMapping("/logs/{id}/review")
    @RequireRole({0})
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ContentReviewDTO dto) {
        interactionLogService.review(id, dto, true);
        return Result.ok();
    }

    /** 内容安全概览：近 N 天调用量 / 拦截量 / 替换量 / 降级量 / 待复核量 */
    @GetMapping("/summary")
    @RequireRole({0})
    public Result<Map<String, Object>> summary(@RequestParam(required = false) Integer days) {
        return Result.ok(interactionLogService.summary(days == null ? 7 : days));
    }
}
