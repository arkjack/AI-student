package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.SubmissionSaveDTO;
import com.edu.aienlighten.entity.Assignment;
import com.edu.aienlighten.entity.ClassApply;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.Submission;
import com.edu.aienlighten.mapper.AssignmentMapper;
import com.edu.aienlighten.mapper.ClassApplyMapper;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.SubmissionMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.HomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {

    private final ClassStudentMapper classStudentMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ClassApplyMapper classApplyMapper;

    @Override
    public List<Assignment> myAssignments() {
        Long studentId = UserContext.userId();
        List<ClassStudent> classStudents = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, studentId));
        if (classStudents.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> classIds = classStudents.stream()
                .map(ClassStudent::getClassId)
                .distinct()
                .collect(Collectors.toList());
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                .in(Assignment::getClassId, classIds)
                .orderByDesc(Assignment::getCreatedAt));
    }

    @Override
    public List<Submission> mySubmissions() {
        return submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStudentId, UserContext.userId())
                .orderByDesc(Submission::getSubmittedAt));
    }

    @Override
    public Submission submit(SubmissionSaveDTO dto) {
        Long studentId = UserContext.userId();
        Assignment assignment = assignmentMapper.selectById(dto.getAssignmentId());
        if (assignment == null) {
            throw new BizException(1008, "作业不存在");
        }
        // unique(assignment, student)：已提交则覆盖内容并更新时间
        Submission exist = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, dto.getAssignmentId())
                .eq(Submission::getStudentId, studentId));
        if (exist == null) {
            Submission s = new Submission();
            s.setAssignmentId(dto.getAssignmentId());
            s.setStudentId(studentId);
            s.setContent(dto.getContent());
            s.setStatus(1);
            submissionMapper.insert(s);
            return s;
        } else {
            exist.setContent(dto.getContent());
            exist.setStatus(1);
            exist.setSubmittedAt(LocalDateTime.now());
            submissionMapper.updateById(exist);
            return exist;
        }
    }

    @Override
    public Submission feedback(Long assignmentId) {
        Submission s = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStudentId, UserContext.userId()));
        if (s == null) {
            throw new BizException(1009, "尚未提交该作业");
        }
        return s;
    }

    @Override
    public Map<String, Object> myClassStatus() {
        Long studentId = UserContext.userId();
        ClassStudent cs = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, studentId)
                .orderByAsc(ClassStudent::getId)
                .last("limit 1"));
        if (cs != null) {
            ClassInfo c = classInfoMapper.selectById(cs.getClassId());
            return Map.of("status", "joined", "className", c == null ? "" : c.getName());
        }
        ClassApply apply = classApplyMapper.selectOne(new LambdaQueryWrapper<ClassApply>()
                .eq(ClassApply::getStudentId, studentId)
                .eq(ClassApply::getStatus, 0)
                .orderByDesc(ClassApply::getId)
                .last("limit 1"));
        if (apply != null) {
            ClassInfo c = classInfoMapper.selectById(apply.getClassId());
            return Map.of("status", "pending", "className", c == null ? "" : c.getName());
        }
        return Map.of("status", "none", "className", "");
    }
}
