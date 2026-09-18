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
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.StudentStatsService;
import com.edu.aienlighten.vo.StudentStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentStatsServiceImpl implements StudentStatsService {

    private final CourseProgressMapper courseProgressMapper;
    private final BlocklyProjectMapper blocklyProjectMapper;
    private final AiWritingMapper aiWritingMapper;
    private final QuizRecordMapper quizRecordMapper;
    private final SubmissionMapper submissionMapper;

    @Override
    public StudentStatsVO myStats() {
        Long sid = UserContext.userId();
        StudentStatsVO vo = new StudentStatsVO();
        if (sid == null) {
            return vo;
        }

        // ===== 课程进度：已完成课程数 + 累计观看时长 =====
        List<CourseProgress> progresses = courseProgressMapper.selectList(
                new LambdaQueryWrapper<CourseProgress>().eq(CourseProgress::getStudentId, sid));
        long completedCourses = progresses.stream()
                .filter(p -> (p.getProgress() != null && p.getProgress() >= 100)
                        || (p.getCompleted() != null && p.getCompleted() == 1))
                .count();
        long totalWatchSeconds = progresses.stream()
                .mapToLong(p -> p.getWatchSeconds() == null ? 0 : p.getWatchSeconds())
                .sum();
        vo.setCompletedCourses((int) completedCourses);
        vo.setStudyMinutes(Math.round(totalWatchSeconds / 60.0 * 10) / 10.0);

        // ===== 作品数量（已提交编程作品 + 已提交 AI 写作） =====
        long projectCount = blocklyProjectMapper.selectCount(new LambdaQueryWrapper<BlocklyProject>()
                .eq(BlocklyProject::getStudentId, sid).eq(BlocklyProject::getStatus, 1));
        long writingCount = aiWritingMapper.selectCount(new LambdaQueryWrapper<AiWriting>()
                .eq(AiWriting::getStudentId, sid).eq(AiWriting::getStatus, 1));
        vo.setWorksCount((int) (projectCount + writingCount));

        // ===== 闯关次数 =====
        long quizCount = quizRecordMapper.selectCount(new LambdaQueryWrapper<QuizRecord>()
                .eq(QuizRecord::getStudentId, sid));
        vo.setQuizCount((int) quizCount);

        // ===== 作业：提交次数、平均分、得分趋势 =====
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStudentId, sid)
                .orderByAsc(Submission::getSubmittedAt));
        List<Submission> graded = new ArrayList<>();
        for (Submission s : submissions) {
            if (s.getStatus() != null && s.getStatus() == 2 && s.getScore() != null) {
                graded.add(s);
            }
        }
        vo.setSubmitCount(submissions.size());
        if (!graded.isEmpty()) {
            double avg = graded.stream().mapToInt(Submission::getScore).average().orElse(0);
            vo.setAvgScore((int) Math.round(avg));
        } else {
            vo.setAvgScore(0);
        }
        DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd");
        List<String> scoreLabels = new ArrayList<>();
        List<Integer> scoreValues = new ArrayList<>();
        for (Submission s : graded) {
            scoreLabels.add(s.getSubmittedAt() == null ? "" : s.getSubmittedAt().format(md));
            scoreValues.add(s.getScore());
        }
        vo.setScoreLabels(scoreLabels);
        vo.setScoreValues(scoreValues);

        // ===== 成果分布 =====
        List<String> distLabels = new ArrayList<>();
        List<Integer> distValues = new ArrayList<>();
        distLabels.add("编程作品");
        distValues.add((int) projectCount);
        distLabels.add("AI 写作");
        distValues.add((int) writingCount);
        distLabels.add("闯关");
        distValues.add((int) quizCount);
        vo.setDistLabels(distLabels);
        vo.setDistValues(distValues);

        // ===== 最近 7 天学习动态 =====
        fillWeekActivity(vo, sid);

        // ===== 成长等级（经验值） =====
        int exp = (int) (completedCourses * 50 + vo.getWorksCount() * 20 + graded.size() * 10 + quizCount * 10);
        int level = exp / 200 + 1;
        vo.setExp(exp);
        vo.setLevel(level);
        vo.setExpNext(level * 200);

        // ===== 本月成就 =====
        List<StudentStatsVO.MonthGoal> goals = new ArrayList<>();
        goals.add(goal("完成 3 个作品", vo.getWorksCount(), 3));
        goals.add(goal("闯关 5 次", vo.getQuizCount(), 5));
        goals.add(goal("完成 4 门课程", vo.getCompletedCourses(), 4));
        goals.add(goal("提交 5 次作业", vo.getSubmitCount(), 5));
        vo.setMonthGoals(goals);

        return vo;
    }

    private StudentStatsVO.MonthGoal goal(String name, int cur, int goal) {
        StudentStatsVO.MonthGoal g = new StudentStatsVO.MonthGoal();
        g.setName(name);
        g.setCur(cur);
        g.setGoal(goal);
        return g;
    }

    /** 填充最近 7 天学习动态（每日活跃次数） */
    private void fillWeekActivity(StudentStatsVO vo, Long sid) {
        DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        Map<LocalDate, Integer> dayCount = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dayCount.put(start.plusDays(i), 0);
        }

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        List<LocalDate> dates = new ArrayList<>();
        courseProgressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                        .eq(CourseProgress::getStudentId, sid)
                        .ge(CourseProgress::getUpdatedAt, startTime)
                        .lt(CourseProgress::getUpdatedAt, endTime))
                .forEach(p -> { if (p.getUpdatedAt() != null) dates.add(p.getUpdatedAt().toLocalDate()); });
        quizRecordMapper.selectList(new LambdaQueryWrapper<QuizRecord>()
                        .eq(QuizRecord::getStudentId, sid)
                        .ge(QuizRecord::getFinishedAt, startTime)
                        .lt(QuizRecord::getFinishedAt, endTime))
                .forEach(r -> { if (r.getFinishedAt() != null) dates.add(r.getFinishedAt().toLocalDate()); });
        submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, sid)
                        .ge(Submission::getSubmittedAt, startTime)
                        .lt(Submission::getSubmittedAt, endTime))
                .forEach(s -> { if (s.getSubmittedAt() != null) dates.add(s.getSubmittedAt().toLocalDate()); });
        blocklyProjectMapper.selectList(new LambdaQueryWrapper<BlocklyProject>()
                        .eq(BlocklyProject::getStudentId, sid)
                        .ge(BlocklyProject::getSubmittedAt, startTime)
                        .lt(BlocklyProject::getSubmittedAt, endTime))
                .forEach(p -> { if (p.getSubmittedAt() != null) dates.add(p.getSubmittedAt().toLocalDate()); });
        aiWritingMapper.selectList(new LambdaQueryWrapper<AiWriting>()
                        .eq(AiWriting::getStudentId, sid)
                        .ge(AiWriting::getCreatedAt, startTime)
                        .lt(AiWriting::getCreatedAt, endTime))
                .forEach(w -> { if (w.getCreatedAt() != null) dates.add(w.getCreatedAt().toLocalDate()); });

        for (LocalDate d : dates) {
            if (dayCount.containsKey(d)) {
                dayCount.merge(d, 1, Integer::sum);
            }
        }

        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> e : dayCount.entrySet()) {
            labels.add(e.getKey().format(md));
            counts.add(e.getValue());
        }
        vo.setWeekLabels(labels);
        vo.setWeekCounts(counts);
    }
}
