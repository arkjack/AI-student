package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.SubmissionSaveDTO;
import com.edu.aienlighten.entity.Assignment;
import com.edu.aienlighten.entity.Submission;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.HomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 作业提交模块（学生端） */
@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    /** 当前学生所在班级的作业任务（按 classId 列表查询） */
    @GetMapping("/assignments")
    @RequireRole({2})
    public Result<List<Assignment>> assignments() {
        return Result.ok(homeworkService.myAssignments());
    }

    /** 我的提交记录 */
    @GetMapping("/submissions")
    @RequireRole({2})
    public Result<List<Submission>> submissions() {
        return Result.ok(homeworkService.mySubmissions());
    }

    /** 提交作业（已提交则覆盖内容并更新时间） */
    @PostMapping("/submit")
    @RequireRole({2})
    public Result<Submission> submit(@Valid @RequestBody SubmissionSaveDTO dto) {
        return Result.ok(homeworkService.submit(dto));
    }

    /** 查看某作业的批改反馈 */
    @GetMapping("/feedback")
    @RequireRole({2})
    public Result<Submission> feedback(@RequestParam Long assignmentId) {
        return Result.ok(homeworkService.feedback(assignmentId));
    }

    /** 我的班级状态（已入班 / 待审批 / 未申请） */
    @GetMapping("/my-class")
    @RequireRole({2})
    public Result<Map<String, Object>> myClass() {
        return Result.ok(homeworkService.myClassStatus());
    }
}
