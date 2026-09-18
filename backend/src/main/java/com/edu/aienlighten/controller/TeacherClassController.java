package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ClassCreateDTO;
import com.edu.aienlighten.dto.ClassStudentAddDTO;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.TeacherClassService;
import com.edu.aienlighten.vo.ClassApplyVO;
import com.edu.aienlighten.vo.ClassStudentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 教师端 · 班级管理 */
@RestController
@RequestMapping("/api/teacher/classes")
@RequiredArgsConstructor
public class TeacherClassController {

    private final TeacherClassService teacherClassService;

    /** 我的班级列表 */
    @GetMapping
    @RequireRole({0, 1})
    public Result<List<ClassInfo>> list() {
        return Result.ok(teacherClassService.listMyClasses());
    }

    /** 创建班级 */
    @PostMapping
    @RequireRole({0, 1})
    public Result<ClassInfo> create(@Valid @RequestBody ClassCreateDTO dto) {
        return Result.ok(teacherClassService.create(dto));
    }

    /** 编辑班级 */
    @PutMapping("/{id}")
    @RequireRole({0, 1})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ClassCreateDTO dto) {
        teacherClassService.update(id, dto);
        return Result.ok();
    }

    /** 删除班级 */
    @DeleteMapping("/{id}")
    @RequireRole({0, 1})
    public Result<Void> delete(@PathVariable Long id) {
        teacherClassService.delete(id);
        return Result.ok();
    }

    /** 班级学生列表 */
    @GetMapping("/{id}/students")
    @RequireRole({0, 1})
    public Result<List<ClassStudentVO>> students(@PathVariable Long id) {
        return Result.ok(teacherClassService.listStudents(id));
    }

    /** 添加学生 */
    @PostMapping("/{id}/students")
    @RequireRole({0, 1})
    public Result<Void> addStudent(@PathVariable Long id, @Valid @RequestBody ClassStudentAddDTO dto) {
        teacherClassService.addStudent(id, dto.getStudentId());
        return Result.ok();
    }

    /** 移出班级 */
    @DeleteMapping("/stu/{classStudentId}")
    @RequireRole({0, 1})
    public Result<Void> removeStudent(@PathVariable Long classStudentId) {
        teacherClassService.removeStudent(classStudentId);
        return Result.ok();
    }

    /** 入班申请列表 */
    @GetMapping("/applies")
    @RequireRole({0, 1})
    public Result<List<ClassApplyVO>> applies() {
        return Result.ok(teacherClassService.listApplies());
    }

    /** 同意入班申请 */
    @PostMapping("/applies/{applyId}/approve")
    @RequireRole({0, 1})
    public Result<Void> approve(@PathVariable Long applyId) {
        teacherClassService.approveApply(applyId);
        return Result.ok();
    }

    /** 拒绝入班申请 */
    @PostMapping("/applies/{applyId}/reject")
    @RequireRole({0, 1})
    public Result<Void> reject(@PathVariable Long applyId) {
        teacherClassService.rejectApply(applyId);
        return Result.ok();
    }
}
