package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.AiWriting;
import com.edu.aienlighten.entity.BlocklyProject;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.entity.QuizRecord;
import com.edu.aienlighten.entity.Submission;
import com.edu.aienlighten.mapper.AiWritingMapper;
import com.edu.aienlighten.mapper.BlocklyProjectMapper;
import com.edu.aienlighten.mapper.CourseProgressMapper;
import com.edu.aienlighten.mapper.QuizRecordMapper;
import com.edu.aienlighten.mapper.SubmissionMapper;
import com.edu.aienlighten.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    /** 连续学习/最后活跃的统计窗口上限，足够覆盖任何现实的连续记录 */
    private static final int WINDOW_DAYS = 400;

    private final CourseProgressMapper courseProgressMapper;
    private final QuizRecordMapper quizRecordMapper;
    private final SubmissionMapper submissionMapper;
    private final BlocklyProjectMapper blocklyProjectMapper;
    private final AiWritingMapper aiWritingMapper;

    @Override
    public List<LocalDate> collectActivityDates(Long studentId, LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDate> dates = new ArrayList<>();
        if (studentId == null) {
            return dates;
        }
        courseProgressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                        .eq(CourseProgress::getStudentId, studentId)
                        .ge(CourseProgress::getUpdatedAt, startTime)
                        .lt(CourseProgress::getUpdatedAt, endTime))
                .forEach(p -> { if (p.getUpdatedAt() != null) dates.add(p.getUpdatedAt().toLocalDate()); });
        quizRecordMapper.selectList(new LambdaQueryWrapper<QuizRecord>()
                        .eq(QuizRecord::getStudentId, studentId)
                        .ge(QuizRecord::getFinishedAt, startTime)
                        .lt(QuizRecord::getFinishedAt, endTime))
                .forEach(r -> { if (r.getFinishedAt() != null) dates.add(r.getFinishedAt().toLocalDate()); });
        submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, studentId)
                        .ge(Submission::getSubmittedAt, startTime)
                        .lt(Submission::getSubmittedAt, endTime))
                .forEach(s -> { if (s.getSubmittedAt() != null) dates.add(s.getSubmittedAt().toLocalDate()); });
        blocklyProjectMapper.selectList(new LambdaQueryWrapper<BlocklyProject>()
                        .eq(BlocklyProject::getStudentId, studentId)
                        .ge(BlocklyProject::getSubmittedAt, startTime)
                        .lt(BlocklyProject::getSubmittedAt, endTime))
                .forEach(p -> { if (p.getSubmittedAt() != null) dates.add(p.getSubmittedAt().toLocalDate()); });
        aiWritingMapper.selectList(new LambdaQueryWrapper<AiWriting>()
                        .eq(AiWriting::getStudentId, studentId)
                        .ge(AiWriting::getCreatedAt, startTime)
                        .lt(AiWriting::getCreatedAt, endTime))
                .forEach(w -> { if (w.getCreatedAt() != null) dates.add(w.getCreatedAt().toLocalDate()); });
        return dates;
    }

    @Override
    public LocalDate lastActivityDate(Long studentId) {
        if (studentId == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return collectActivityDates(studentId,
                        today.minusDays(WINDOW_DAYS).atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().max(LocalDate::compareTo).orElse(null);
    }

    @Override
    public int streakDays(Long studentId) {
        if (studentId == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>(collectActivityDates(studentId,
                today.minusDays(WINDOW_DAYS).atStartOfDay(), today.plusDays(1).atStartOfDay()));

        // 今天尚未产生行为时从昨天起算 —— 否则每天早上打开首页都会看到 0，体验上像坏了
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
