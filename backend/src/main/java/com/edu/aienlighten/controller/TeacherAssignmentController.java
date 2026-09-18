package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AssignmentCreateDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.TeacherAssignmentService;
import com.edu.aienlighten.vo.AssignmentVO;
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

import java.util.List;

/** 教师端 · 任务布置 */
@RestController
@RequestMapping("/api/teacher/assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    /** 我的任务列表 */
    @GetMapping
    @RequireRole({0, 1})
    public Result<List<AssignmentVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(teacherAssignmentService.listMyAssignments(status));
    }

    /** 布置任务（可针对多个班级） */
    @PostMapping
    @RequireRole({0, 1})
    public Result<Void> create(@Valid @RequestBody AssignmentCreateDTO dto) {
        teacherAssignmentService.create(dto);
        return Result.ok();
    }

    /** 截止任务 */
    @PutMapping("/{id}/close")
    @RequireRole({0, 1})
    public Result<Void> close(@PathVariable Long id) {
        teacherAssignmentService.close(id);
        return Result.ok();
    }

    /** 删除任务 */
    @DeleteMapping("/{id}")
    @RequireRole({0, 1})
    public Result<Void> delete(@PathVariable Long id) {
        teacherAssignmentService.delete(id);
        return Result.ok();
    }
}
