package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.ProgressSaveDTO;
import com.edu.aienlighten.entity.Course;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.mapper.CourseMapper;
import com.edu.aienlighten.mapper.CourseProgressMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.service.ProgressService;
import com.edu.aienlighten.vo.CourseProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final CourseProgressMapper courseProgressMapper;
    private final CourseMapper courseMapper;
    private final ExpService expService;

    @Override
    public List<CourseProgressVO> myProgress() {
        Long studentId = UserContext.userId();
        List<CourseProgress> list = courseProgressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getStudentId, studentId)
                .orderByDesc(CourseProgress::getUpdatedAt));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> courseIds = list.stream()
                .map(CourseProgress::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        return list.stream()
                .map(p -> {
                    Course course = courseMap.get(p.getCourseId());
                    return CourseProgressVO.builder()
                            .id(p.getId())
                            .courseId(p.getCourseId())
                            .courseTitle(course == null ? null : course.getTitle())
                            .coverEmoji(course == null ? null : course.getCoverEmoji())
                            .progress(p.getProgress())
                            .watchSeconds(p.getWatchSeconds())
                            .completed(p.getCompleted())
                            .updatedAt(p.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public CourseProgress saveProgress(ProgressSaveDTO dto) {
        Long studentId = UserContext.userId();
        CourseProgress exist = courseProgressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getStudentId, studentId)
                .eq(CourseProgress::getCourseId, dto.getCourseId()));
        int progress = dto.getProgress() == null ? 0 : dto.getProgress();
        int completed = progress >= 100 ? 1 : 0;
        int watchSeconds = dto.getWatchSeconds() == null ? 0 : dto.getWatchSeconds();
        CourseProgress saved;
        if (exist == null) {
            CourseProgress p = new CourseProgress();
            p.setStudentId(studentId);
            p.setCourseId(dto.getCourseId());
            p.setProgress(progress);
            p.setWatchSeconds(watchSeconds);
            p.setCompleted(completed);
            courseProgressMapper.insert(p);
            saved = p;
        } else {
            exist.setProgress(progress);
            exist.setWatchSeconds(watchSeconds);
            exist.setCompleted(completed);
            courseProgressMapper.updateById(exist);
            saved = exist;
        }
        awardCourseExp(studentId, dto.getCourseId(), progress);
        return saved;
    }

    /**
     * 看课经验。
     *
     * <p>里程碑与完成奖励都靠 (student_id, source_key) 唯一键幂等 ——
     * 进度只增不减，所以这里用 {@code progress >= 阈值} 判断即可，
     * 反复上报同一进度不会重复给分。
     */
    private void awardCourseExp(Long studentId, Long courseId, int progress) {
        if (progress >= 25) {
            expService.award(studentId, "course", "course:" + courseId + ":p25",
                    ExpService.EXP_COURSE_MILESTONE, "看课进度达到 25%");
        }
        if (progress >= 50) {
            expService.award(studentId, "course", "course:" + courseId + ":p50",
                    ExpService.EXP_COURSE_MILESTONE, "看课进度达到 50%");
        }
        if (progress >= 75) {
            expService.award(studentId, "course", "course:" + courseId + ":p75",
                    ExpService.EXP_COURSE_MILESTONE_LATE, "看课进度达到 75%");
        }
        if (progress >= 100) {
            expService.award(studentId, "course", "course:" + courseId + ":done",
                    ExpService.EXP_COURSE_DONE, "完成课程");
        }
        expService.onStudyAction(studentId);
    }
}
