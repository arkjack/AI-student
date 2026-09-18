package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.CourseDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminCourseService;
import com.edu.aienlighten.vo.CourseVO;
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

/** 管理端 · 课程管理 */
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    /** 课程列表（含分类名） */
    @GetMapping
    @RequireRole({0})
    public Result<List<CourseVO>> list(@RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(adminCourseService.list(categoryId, status, keyword));
    }

    /** 新增课程 */
    @PostMapping
    @RequireRole({0})
    public Result<CourseVO> create(@Valid @RequestBody CourseDTO dto) {
        return Result.ok(adminCourseService.create(dto));
    }

    /** 编辑课程（含上下架） */
    @PutMapping("/{id}")
    @RequireRole({0})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        adminCourseService.update(id, dto);
        return Result.ok();
    }

    /** 删除课程 */
    @DeleteMapping("/{id}")
    @RequireRole({0})
    public Result<Void> delete(@PathVariable Long id) {
        adminCourseService.delete(id);
        return Result.ok();
    }
}
