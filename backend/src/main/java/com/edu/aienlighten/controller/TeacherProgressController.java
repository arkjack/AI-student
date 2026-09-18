package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.TeacherAssignmentService;
import com.edu.aienlighten.vo.AssignmentProgressVO;
import com.edu.aienlighten.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 教师端 · 进度追踪 */
@RestController
@RequestMapping("/api/teacher/progress")
@RequiredArgsConstructor
public class TeacherProgressController {

    private final TeacherAssignmentService teacherAssignmentService;

    /** 班级学生学习概况 */
    @GetMapping("/students")
    @RequireRole({0, 1})
    public Result<List<StudentProgressVO>> students(@RequestParam Long classId) {
        return Result.ok(teacherAssignmentService.classProgress(classId));
    }

    /** 任务提交情况 */
    @GetMapping("/assignment/{assignmentId}")
    @RequireRole({0, 1})
    public Result<List<AssignmentProgressVO>> assignment(@PathVariable Long assignmentId) {
        return Result.ok(teacherAssignmentService.assignmentProgress(assignmentId));
    }
}
