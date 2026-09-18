package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.QuizRecordDTO;
import com.edu.aienlighten.entity.Assignment;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.QuizQuestion;
import com.edu.aienlighten.entity.QuizRecord;
import com.edu.aienlighten.entity.Submission;
import com.edu.aienlighten.mapper.AssignmentMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.QuizQuestionMapper;
import com.edu.aienlighten.mapper.QuizRecordMapper;
import com.edu.aienlighten.mapper.SubmissionMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizRecordMapper quizRecordMapper;
    private final ClassStudentMapper classStudentMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final ExpService expService;

    @Override
    public List<QuizQuestion> questions(String level) {
        String lv = (level == null || level.trim().isEmpty()) ? "AI 基础" : level.trim();
        return quizQuestionMapper.selectList(new LambdaQueryWrapper<QuizQuestion>()
                .eq(QuizQuestion::getLevel, lv)
                .orderByAsc(QuizQuestion::getSort));
    }

    @Override
    public List<String> levels() {
        return quizQuestionMapper.selectList(null).stream()
                .map(QuizQuestion::getLevel)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public QuizRecord saveRecord(QuizRecordDTO dto) {
        QuizRecord r = new QuizRecord();
        r.setStudentId(UserContext.userId());
        r.setLevel(dto.getLevel());
        r.setScore(dto.getScore() == null ? 0 : dto.getScore());
        r.setCorrectCount(dto.getCorrectCount() == null ? 0 : dto.getCorrectCount());
        r.setTotalCount(dto.getTotalCount() == null ? 0 : dto.getTotalCount());
        quizRecordMapper.insert(r);
        // 关联 AI 互动实验任务：完成闯关后，为该学生的作业任务生成提交记录
        try {
            linkAiTask(r, dto.getAssignmentId());
        } catch (Exception e) {
            // 关联失败不影响成绩记录
        }
        awardQuiz(r);
        return r;
    }

    /** 闯关经验：完成 + 满分额外奖励。sourceKey 带记录主键，每次闯关独立计分。 */
    private void awardQuiz(QuizRecord r) {
        Long sid = r.getStudentId();
        expService.award(sid, "quiz", "quiz:" + r.getId(),
                ExpService.EXP_QUIZ_DONE, "完成知识闯关");
        boolean perfect = r.getTotalCount() != null && r.getTotalCount() > 0
                && Objects.equals(r.getCorrectCount(), r.getTotalCount());
        if (perfect) {
            expService.award(sid, "quiz", "quiz:" + r.getId() + ":perfect",
                    ExpService.EXP_QUIZ_PERFECT, "知识闯关全部答对");
        }
        expService.onStudyAction(sid);
    }

    @Override
    public List<QuizRecord> myRecords() {
        return quizRecordMapper.selectList(new LambdaQueryWrapper<QuizRecord>()
                .eq(QuizRecord::getStudentId, UserContext.userId())
                .orderByDesc(QuizRecord::getId));
    }

    /**
     * 将闯关完成关联到作业任务的提交：
     * 优先用指定的 assignmentId；否则自动找该学生班级的 type=3 任务。
     */
    private void linkAiTask(QuizRecord r, Long assignmentId) {
        if (assignmentId != null) {
            linkOneTask(r, assignmentId);
            return;
        }
        List<ClassStudent> cs = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, r.getStudentId()));
        if (cs.isEmpty()) {
            return;
        }
        List<Long> classIds = cs.stream().map(ClassStudent::getClassId).distinct().collect(Collectors.toList());
        List<Assignment> tasks = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                .in(Assignment::getClassId, classIds)
                .eq(Assignment::getType, 3));
        for (Assignment task : tasks) {
            linkOneTask(r, task.getId());
        }
    }

    private void linkOneTask(QuizRecord r, Long assignmentId) {
        Long count = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStudentId, r.getStudentId()));
        if (count == 0) {
            Submission s = new Submission();
            s.setAssignmentId(assignmentId);
            s.setStudentId(r.getStudentId());
            s.setContent("闯关成绩：" + r.getScore() + " 分，答对 " + r.getCorrectCount() + "/" + r.getTotalCount() + " 题");
            s.setStatus(1);
            submissionMapper.insert(s);
        }
    }
}
