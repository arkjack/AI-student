package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.entity.Course;
import com.edu.aienlighten.entity.CourseCategory;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.mapper.CourseCategoryMapper;
import com.edu.aienlighten.mapper.CourseMapper;
import com.edu.aienlighten.mapper.CourseProgressMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.CourseService;
import com.edu.aienlighten.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseCategoryMapper courseCategoryMapper;
    private final CourseProgressMapper courseProgressMapper;

    @Override
    public List<CourseVO> listCourses(Long categoryId, Integer difficulty, String keyword) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        qw.eq(Course::getStatus, 1); // 已上架
        if (categoryId != null) {
            qw.eq(Course::getCategoryId, categoryId);
        }
        if (difficulty != null) {
            qw.eq(Course::getDifficulty, difficulty);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like(Course::getTitle, keyword.trim());
        }
        qw.orderByDesc(Course::getCreatedAt);
        Map<Long, String> catNames = categoryNameMap();
        List<CourseVO> vos = courseMapper.selectList(qw).stream()
                .map(c -> toVO(c, catNames.get(c.getCategoryId())))
                .collect(Collectors.toList());
        fillProgress(vos);
        return vos;
    }

    /** 回填当前用户（学生）对每门课的学习进度 */
    private void fillProgress(List<CourseVO> vos) {
        Long userId = UserContext.userId();
        if (userId == null || vos.isEmpty()) {
            return;
        }
        List<Long> courseIds = vos.stream().map(CourseVO::getId).collect(Collectors.toList());
        List<CourseProgress> list = courseProgressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getStudentId, userId)
                .in(CourseProgress::getCourseId, courseIds));
        Map<Long, CourseProgress> map = list.stream()
                .collect(Collectors.toMap(CourseProgress::getCourseId, p -> p, (a, b) -> a));
        for (CourseVO vo : vos) {
            CourseProgress cp = map.get(vo.getId());
            if (cp != null) {
                vo.setProgress(cp.getProgress() == null ? 0 : cp.getProgress());
                vo.setCompleted(cp.getCompleted() == null ? 0 : cp.getCompleted());
            }
        }
    }

    @Override
    public CourseVO getCourseDetail(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(1006, "课程不存在");
        }
        CourseCategory category = courseCategoryMapper.selectById(course.getCategoryId());
        String categoryName = category == null ? null : category.getName();
        CourseVO vo = toVO(course, categoryName);
        // 回填当前用户（学生）的学习进度，供前端刷新后恢复
        Long userId = UserContext.userId();
        if (userId != null) {
            CourseProgress cp = courseProgressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                    .eq(CourseProgress::getStudentId, userId)
                    .eq(CourseProgress::getCourseId, id));
            if (cp != null) {
                vo.setProgress(cp.getProgress() == null ? 0 : cp.getProgress());
                vo.setCompleted(cp.getCompleted() == null ? 0 : cp.getCompleted());
            }
        }
        return vo;
    }

    @Override
    public List<CourseCategory> listCategories() {
        return courseCategoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                .orderByAsc(CourseCategory::getSort));
    }

    private Map<Long, String> categoryNameMap() {
        return courseCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(CourseCategory::getId, CourseCategory::getName, (a, b) -> a));
    }

    private CourseVO toVO(Course course, String categoryName) {
        return CourseVO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .categoryId(course.getCategoryId())
                .categoryName(categoryName)
                .difficulty(course.getDifficulty())
                .durationMinutes(course.getDurationMinutes())
                .coverEmoji(course.getCoverEmoji())
                .coverImage(course.getCoverImage())
                .summary(course.getSummary())
                .videoUrl(course.getVideoUrl())
                .teacher(course.getTeacher())
                .views(course.getViews())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .build();
    }
}
