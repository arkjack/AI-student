package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.AssignmentCreateDTO;
import com.edu.aienlighten.dto.ReviewDTO;
import com.edu.aienlighten.entity.Assignment;
import com.edu.aienlighten.entity.AiWriting;
import com.edu.aienlighten.entity.BlocklyTemplate;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.Course;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.entity.Submission;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.AiWritingMapper;
import com.edu.aienlighten.mapper.AssignmentMapper;
import com.edu.aienlighten.mapper.BlocklyTemplateMapper;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.CourseMapper;
import com.edu.aienlighten.mapper.CourseProgressMapper;
import com.edu.aienlighten.mapper.SubmissionMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.service.TeacherAssignmentService;
import com.edu.aienlighten.vo.AssignmentProgressVO;
import com.edu.aienlighten.vo.AssignmentSubmissionVO;
import com.edu.aienlighten.vo.AssignmentVO;
import com.edu.aienlighten.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final AiWritingMapper aiWritingMapper;
    private final CourseProgressMapper courseProgressMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final BlocklyTemplateMapper blocklyTemplateMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    @Override
    public List<AssignmentVO> listMyAssignments(Integer status) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getTeacherId, UserContext.userId())
                .orderByDesc(Assignment::getId);
        if (status != null) {
            w.eq(Assignment::getStatus, status);
        }
        List<Assignment> list = assignmentMapper.selectList(w);
        if (list.isEmpty()) {
            return List.of();
        }
        // 班级 id -> 名称
        Set<Long> classIds = list.stream().map(Assignment::getClassId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> classMap = classIds.isEmpty() ? Map.of()
                : classInfoMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getName, (a, b) -> a));
        // 资源（课程）id -> 名称
        Set<Long> resIds = list.stream().map(Assignment::getResourceId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> courseMap = resIds.isEmpty() ? Map.of()
                : courseMapper.selectBatchIds(resIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));
        return list.stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setType(a.getType());
            vo.setClassId(a.getClassId());
            vo.setClassName(a.getClassId() == null ? null : classMap.get(a.getClassId()));
            vo.setResourceId(a.getResourceId());
            vo.setResourceName(a.getResourceId() == null ? null : courseMap.get(a.getResourceId()));
            vo.setContent(a.getContent());
            vo.setDeadline(a.getDeadline());
            vo.setStatus(a.getStatus());
            // 完成人数：课程学习任务按课程观看进度统计，其余按作业提交统计
            long doneCount;
            if (a.getType() != null && a.getType() == 1 && a.getResourceId() != null) {
                List<Long> studentIds = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, a.getClassId()))
                        .stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
                doneCount = studentIds.isEmpty() ? 0 : courseProgressMapper.selectCount(new LambdaQueryWrapper<CourseProgress>()
                        .in(CourseProgress::getStudentId, studentIds)
                        .eq(CourseProgress::getCourseId, a.getResourceId())
                        .ge(CourseProgress::getProgress, 100));
            } else {
                doneCount = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, a.getId()));
            }
            vo.setSubmissionCount(doneCount);
            vo.setTotalCount(classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, a.getClassId())));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void create(AssignmentCreateDTO dto) {
        Long teacherId = UserContext.userId();
        // 编程实验任务：关联资源为积木模板，若已停用则禁止发布
        if (dto.getType() != null && dto.getType() == 2) {
            String resourceName = resourceNameFromContent(dto.getContent());
            if (resourceName != null) {
                BlocklyTemplate t = blocklyTemplateMapper.selectOne(new LambdaQueryWrapper<BlocklyTemplate>()
                        .eq(BlocklyTemplate::getName, resourceName)
                        .last("limit 1"));
                if (t != null && t.getEnabled() != null && t.getEnabled() != 1) {
                    throw new BizException("当前资源已关闭请联系管理员");
                }
            }
        }
        for (Long classId : dto.getClassIds()) {
            Assignment a = new Assignment();
            a.setTeacherId(teacherId);
            a.setClassId(classId);
            a.setTitle(dto.getTitle());
            a.setContent(dto.getContent());
            a.setType(dto.getType() == null ? 1 : dto.getType());
            a.setResourceId(dto.getResourceId());
            a.setDeadline(dto.getDeadline());
            a.setStatus(1);
            assignmentMapper.insert(a);
            // 通知该班学生：布置了新任务
            List<Long> studentIds = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, classId))
                    .stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
            for (Long sid : studentIds) {
                notificationService.push(sid, "task", "新任务",
                        "老师布置了新任务「" + dto.getTitle() + "」，记得去完成哦", "/student/homework");
            }
        }
        operationLogService.record("布置任务", "任务：" + dto.getTitle() + "，班级数：" + dto.getClassIds().size());
    }

    /** 从任务内容里取出资源名（内容格式：资源名；要求，或资源名） */
    private String resourceNameFromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String first = content.split("；")[0].trim();
        return first.isEmpty() ? null : first;
    }

    @Override
    public void close(Long id) {
        Assignment a = requireOwned(id);
        a.setStatus(0);
        assignmentMapper.updateById(a);
        operationLogService.record("截止任务", "任务：" + a.getTitle());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Assignment a = requireOwned(id);
        submissionMapper.delete(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, id));
        assignmentMapper.deleteById(id);
        operationLogService.record("删除任务", "任务：" + a.getTitle());
    }

    @Override
    public List<StudentProgressVO> classProgress(Long classId) {
        requireOwnedClass(classId);
        List<ClassStudent> relations = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId)
                        .orderByAsc(ClassStudent::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = relations.stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return relations.stream().map(r -> {
            Long sid = r.getStudentId();
            User u = userMap.get(sid);
            StudentProgressVO vo = new StudentProgressVO();
            vo.setStudentId(sid);
            if (u != null) {
                vo.setRealName(u.getRealName());
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
            }
            vo.setCompletedCourses(courseProgressMapper.selectCount(new LambdaQueryWrapper<CourseProgress>()
                    .eq(CourseProgress::getStudentId, sid)
                    .eq(CourseProgress::getCompleted, 1)));
            vo.setSubmissions(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getStudentId, sid)));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AssignmentProgressVO> assignmentProgress(Long assignmentId) {
        Assignment a = requireOwned(assignmentId);
        return buildProgress(a);
    }

    private List<AssignmentProgressVO> buildProgress(Assignment a) {
        List<ClassStudent> relations = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, a.getClassId())
                        .orderByAsc(ClassStudent::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = relations.stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        // 课程学习任务（type=1）：按课程观看进度判定完成，而非作业提交
        boolean courseTask = a.getType() != null && a.getType() == 1 && a.getResourceId() != null;
        return relations.stream().map(r -> {
            AssignmentProgressVO vo = new AssignmentProgressVO();
            vo.setStudentId(r.getStudentId());
            User u = userMap.get(r.getStudentId());
            vo.setStudentName(u == null ? null : u.getRealName());
            if (courseTask) {
                CourseProgress cp = courseProgressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                        .eq(CourseProgress::getStudentId, r.getStudentId())
                        .eq(CourseProgress::getCourseId, a.getResourceId()));
                if (cp != null && cp.getProgress() != null && cp.getProgress() >= 100) {
                    vo.setSubmitted(true);
                    vo.setStatus(2);
                    vo.setSubmittedAt(cp.getUpdatedAt());
                } else {
                    vo.setSubmitted(false);
                }
                return vo;
            }
            Submission s = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getAssignmentId, a.getId())
                    .eq(Submission::getStudentId, r.getStudentId()));
            if (s != null) {
                vo.setSubmitted(true);
                vo.setStatus(s.getStatus());
                vo.setScore(s.getScore());
                vo.setFeedback(s.getFeedback());
                vo.setSubmittedAt(s.getSubmittedAt());
            } else {
                vo.setSubmitted(false);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AssignmentSubmissionVO> reviewList(Long assignmentId, Integer status) {
        List<Long> assignmentIds;
        if (assignmentId != null) {
            Assignment a = requireOwned(assignmentId);
            assignmentIds = List.of(a.getId());
        } else {
            // 未指定任务：返回该教师全部任务的提交
            List<Assignment> mine = assignmentMapper.selectList(
                    new LambdaQueryWrapper<Assignment>().eq(Assignment::getTeacherId, UserContext.userId()));
            assignmentIds = mine.stream().map(Assignment::getId).collect(Collectors.toList());
            if (assignmentIds.isEmpty()) {
                return List.of();
            }
        }
        LambdaQueryWrapper<Submission> w = new LambdaQueryWrapper<Submission>()
                .in(Submission::getAssignmentId, assignmentIds)
                .orderByDesc(Submission::getId);
        if (status != null) {
            w.eq(Submission::getStatus, status);
        }
        List<Submission> list = submissionMapper.selectList(w);
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = list.stream().map(Submission::getStudentId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream().map(s -> {
            AssignmentSubmissionVO vo = new AssignmentSubmissionVO();
            vo.setSubmissionId(s.getId());
            vo.setAssignmentId(s.getAssignmentId());
            vo.setStudentId(s.getStudentId());
            User u = userMap.get(s.getStudentId());
            vo.setStudentName(u == null ? null : u.getRealName());
            vo.setStatus(s.getStatus());
            vo.setContent(s.getContent());
            vo.setScore(s.getScore());
            vo.setFeedback(s.getFeedback());
            vo.setSubmittedAt(s.getSubmittedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void review(Long submissionId, ReviewDTO dto) {
        Submission s = submissionMapper.selectById(submissionId);
        if (s == null) {
            throw new BizException("提交记录不存在");
        }
        Assignment a = assignmentMapper.selectById(s.getAssignmentId());
        if (a == null || !a.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("无权批改该作业");
        }
        s.setStatus(2);
        s.setScore(dto.getScore());
        s.setFeedback(dto.getFeedback());
        submissionMapper.updateById(s);
        // 通知学生：作业已批改
        if (a.getType() != null && a.getType() != 1) {
            notificationService.push(s.getStudentId(), "grade", "作业已批改",
                    "你的「" + (a.getTitle() == null ? "作业" : a.getTitle()) + "」已批改，得分 " + dto.getScore() + " 分",
                    "/student/homework");
        }
        // 若为 AI 实验（写作）任务，同步更新学生最新写作的批改分数与评语
        if (a.getType() != null && a.getType() == 3) {
            try {
                AiWriting w = aiWritingMapper.selectOne(new LambdaQueryWrapper<AiWriting>()
                        .eq(AiWriting::getStudentId, s.getStudentId())
                        .eq(AiWriting::getStatus, 1)
                        .orderByDesc(AiWriting::getId)
                        .last("limit 1"));
                if (w != null) {
                    w.setScore(dto.getScore());
                    w.setFeedback(dto.getFeedback());
                    aiWritingMapper.updateById(w);
                }
            } catch (Exception e) {
                // 同步失败不影响批改主流程
            }
        }
        operationLogService.record("作业批改", "作业 id=" + a.getId() + "，学生 id=" + s.getStudentId() + "，得分=" + dto.getScore());
    }

    @Override
    public long pendingCount() {
        List<Assignment> mine = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>().eq(Assignment::getTeacherId, UserContext.userId()));
        if (mine.isEmpty()) {
            return 0;
        }
        List<Long> assignmentIds = mine.stream().map(Assignment::getId).collect(Collectors.toList());
        return submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, 1)
                .in(Submission::getAssignmentId, assignmentIds));
    }

    private Assignment requireOwned(Long assignmentId) {
        Assignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw new BizException("任务不存在");
        }
        if (!a.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("无权操作该任务");
        }
        return a;
    }

    private void requireOwnedClass(Long classId) {
        ClassInfo c = classInfoMapper.selectById(classId);
        if (c == null) {
            throw new BizException("班级不存在");
        }
        if (!c.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("无权查看该班级");
        }
    }
}
