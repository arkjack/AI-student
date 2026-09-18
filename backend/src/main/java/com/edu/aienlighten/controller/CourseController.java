package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.entity.CourseCategory;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.CourseService;
import com.edu.aienlighten.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 课程模块（学生端） */
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /** 已上架课程列表（可根据分类/难度/关键词筛选） */
    @GetMapping("/list")
    @RequireRole({0, 1, 2})
    public Result<List<CourseVO>> list(@RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Integer difficulty,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(courseService.listCourses(categoryId, difficulty, keyword));
    }

    /** 课程详情（含分类名） */
    @GetMapping("/{id}")
    @RequireRole({0, 1, 2})
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.ok(courseService.getCourseDetail(id));
    }

    /** 课程分类列表 */
    @GetMapping("/categories")
    @RequireRole({0, 1, 2})
    public Result<List<CourseCategory>> categories() {
        return Result.ok(courseService.listCategories());
    }
}
