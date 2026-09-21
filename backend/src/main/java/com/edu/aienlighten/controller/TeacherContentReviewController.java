package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ContentReviewDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.InteractionLogService;
import com.edu.aienlighten.vo.AiInteractionLogVO;
import com.edu.aienlighten.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端 · AI 内容复核。
 *
 * <p>与 {@code /api/teacher/review}（作业批改）是两个不同的东西：
 * 作业批改面对的是学生**提交上来**的作品，这里面对的是**每一次 AI 交互**——
 * 包括被安全机制拦截、替换，或被标记为可疑而放行的那部分内容。
 * 教师只能看到自己所带班级学生的记录（由 {@code TeacherScopeService} 限定）。</p>
 */
@RestController
@RequestMapping("/api/teacher/content-review")
@RequiredArgsConstructor
public class TeacherContentReviewController {

    private final InteractionLogService interactionLogService;

    /**
     * 待复核列表。
     *
     * @param reviewStatus 复核状态，缺省为 1（待复核）；传 0 可查看无需复核的记录
     */
    @GetMapping("/list")
    @RequireRole({0, 1})
    public Result<PageVO<AiInteractionLogVO>> list(@RequestParam(required = false) Integer reviewStatus,
                                                   @RequestParam(required = false) String scene,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        return Result.ok(interactionLogService.pageForTeacher(
                reviewStatus == null ? 1 : reviewStatus, scene, keyword, page, size));
    }

    /** 待复核数量（菜单红点） */
    @GetMapping("/pending-count")
    @RequireRole({0, 1})
    public Result<Long> pendingCount() {
        return Result.ok(interactionLogService.pendingCountForTeacher());
    }

    /** 复核详情 */
    @GetMapping("/{id}")
    @RequireRole({0, 1})
    public Result<AiInteractionLogVO> detail(@PathVariable Long id) {
        return Result.ok(interactionLogService.detail(id, false));
    }

    /** 提交复核结论：2 通过 / 3 驳回 */
    @PostMapping("/{id}")
    @RequireRole({0, 1})
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ContentReviewDTO dto) {
        interactionLogService.review(id, dto, false);
        return Result.ok();
    }
}
