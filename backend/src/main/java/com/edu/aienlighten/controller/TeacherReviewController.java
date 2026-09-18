package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ReviewDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.TeacherAssignmentService;
import com.edu.aienlighten.vo.AssignmentSubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 教师端 · 作业批改 */
@RestController
@RequestMapping("/api/teacher/review")
@RequiredArgsConstructor
public class TeacherReviewController {

    private final TeacherAssignmentService teacherAssignmentService;

    /** 提交列表（assignmentId 为空时返回该教师全部任务的提交） */
    @GetMapping("/list")
    @RequireRole({0, 1})
    public Result<List<AssignmentSubmissionVO>> list(@RequestParam(required = false) Long assignmentId,
                                                     @RequestParam(required = false) Integer status) {
        return Result.ok(teacherAssignmentService.reviewList(assignmentId, status));
    }

    /** 批改 */
    @PostMapping("/{submissionId}")
    @RequireRole({0, 1})
    public Result<Void> review(@PathVariable Long submissionId, @Valid @RequestBody ReviewDTO dto) {
        teacherAssignmentService.review(submissionId, dto);
        return Result.ok();
    }

    /** 待批改数 */
    @GetMapping("/pending-count")
    @RequireRole({0, 1})
    public Result<Long> pendingCount() {
        return Result.ok(teacherAssignmentService.pendingCount());
    }
}
