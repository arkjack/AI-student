package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.CourseDTO;
import com.edu.aienlighten.entity.Course;
import com.edu.aienlighten.entity.CourseCategory;
import com.edu.aienlighten.mapper.CourseCategoryMapper;
import com.edu.aienlighten.mapper.CourseMapper;
import com.edu.aienlighten.service.AdminCourseService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseServiceImpl implements AdminCourseService {

    private final CourseMapper courseMapper;
    private final CourseCategoryMapper courseCategoryMapper;
    private final OperationLogService operationLogService;

    @Override
    public List<CourseVO> list(Long categoryId, Integer status, String keyword) {
        LambdaQueryWrapper<Course> w = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            w.eq(Course::getCategoryId, categoryId);
        }
        if (status != null) {
            w.eq(Course::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like(Course::getTitle, keyword);
        }
        w.orderByDesc(Course::getId);
        List<Course> courses = courseMapper.selectList(w);
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = courses.stream().map(Course::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = courseCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CourseCategory::getId, CourseCategory::getName));
        return courses.stream().map(c -> {
            CourseVO vo = new CourseVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setCategoryId(c.getCategoryId());
            vo.setCategoryName(nameMap.get(c.getCategoryId()));
            vo.setDifficulty(c.getDifficulty());
            vo.setDurationMinutes(c.getDurationMinutes());
            vo.setCoverEmoji(c.getCoverEmoji());
            vo.setCoverImage(c.getCoverImage());
            vo.setSummary(c.getSummary());
            vo.setVideoUrl(c.getVideoUrl());
            vo.setTeacher(c.getTeacher());
            vo.setViews(c.getViews());
            vo.setStatus(c.getStatus());
            vo.setCreatedAt(c.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CourseVO create(CourseDTO dto) {
        Course c = new Course();
        c.setTitle(dto.getTitle());
        c.setCategoryId(dto.getCategoryId());
        c.setDifficulty(dto.getDifficulty() == null ? 1 : dto.getDifficulty());
        c.setDurationMinutes(dto.getDurationMinutes() == null ? 15 : dto.getDurationMinutes());
        c.setCoverEmoji(dto.getCoverEmoji());
        c.setCoverImage(dto.getCoverImage());
        c.setSummary(dto.getSummary());
        c.setVideoUrl(dto.getVideoUrl());
        c.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        c.setViews(0);
        courseMapper.insert(c);
        operationLogService.record("新增课程", "课程：" + dto.getTitle());
        return toVO(c, dto.getCategoryId());
    }

    @Override
    public void update(Long id, CourseDTO dto) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException("课程不存在");
        }
        c.setTitle(dto.getTitle());
        c.setCategoryId(dto.getCategoryId());
        c.setDifficulty(dto.getDifficulty() == null ? c.getDifficulty() : dto.getDifficulty());
        c.setDurationMinutes(dto.getDurationMinutes() == null ? c.getDurationMinutes() : dto.getDurationMinutes());
        c.setCoverEmoji(dto.getCoverEmoji());
        c.setCoverImage(dto.getCoverImage());
        c.setSummary(dto.getSummary());
        c.setVideoUrl(dto.getVideoUrl());
        c.setStatus(dto.getStatus() == null ? c.getStatus() : dto.getStatus());
        courseMapper.updateById(c);
        operationLogService.record("编辑课程", "课程：" + dto.getTitle() + "，状态：" + c.getStatus());
    }

    @Override
    public void delete(Long id) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException("课程不存在");
        }
        courseMapper.deleteById(id);
        operationLogService.record("删除课程", "课程：" + c.getTitle());
    }

    private CourseVO toVO(Course c, Long categoryId) {
        CourseVO vo = new CourseVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setCategoryId(c.getCategoryId());
        CourseCategory cc = categoryId == null ? null : courseCategoryMapper.selectById(categoryId);
        vo.setCategoryName(cc == null ? null : cc.getName());
        vo.setDifficulty(c.getDifficulty());
        vo.setDurationMinutes(c.getDurationMinutes());
        vo.setCoverEmoji(c.getCoverEmoji());
        vo.setCoverImage(c.getCoverImage());
        vo.setSummary(c.getSummary());
        vo.setVideoUrl(c.getVideoUrl());
        vo.setTeacher(c.getTeacher());
        vo.setViews(c.getViews());
        vo.setStatus(c.getStatus());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }
}
