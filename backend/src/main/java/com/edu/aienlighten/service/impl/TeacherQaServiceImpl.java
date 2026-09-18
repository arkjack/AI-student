package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.QaRecord;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.QaRecordMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.service.TeacherQaService;
import com.edu.aienlighten.vo.ClassStudentVO;
import com.edu.aienlighten.vo.QaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherQaServiceImpl implements TeacherQaService {

    private final QaRecordMapper qaRecordMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ClassStudentMapper classStudentMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Override
    public List<QaVO> listStudentQuestions() {
        List<Long> studentIds = myStudentIds();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<QaRecord> records = qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .in(QaRecord::getStudentId, studentIds)
                .orderByDesc(QaRecord::getId));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(
                        records.stream().map(QaRecord::getStudentId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return records.stream().map(r -> {
            QaVO vo = new QaVO();
            vo.setId(r.getId());
            vo.setStudentId(r.getStudentId());
            User u = userMap.get(r.getStudentId());
            vo.setStudentName(u == null ? null : u.getRealName());
            vo.setQuestion(r.getQuestion());
            vo.setAnswer(r.getAnswer());
            vo.setAnswerType(r.getAnswerType());
            vo.setTeacherId(r.getTeacherId());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void reply(Long id, String answer) {
        QaRecord r = qaRecordMapper.selectById(id);
        if (r == null) {
            throw new BizException("提问记录不存在");
        }
        r.setAnswer(answer);
        r.setAnswerType(2);
        r.setTeacherId(UserContext.userId());
        qaRecordMapper.updateById(r);
        operationLogService.record("回复答疑", "提问 id=" + id + "，学生 id=" + r.getStudentId());
    }

    @Override
    public List<ClassStudentVO> myClassStudents() {
        List<Long> studentIds = myStudentIds();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return studentIds.stream().map(sid -> {
            User u = userMap.get(sid);
            ClassStudentVO vo = new ClassStudentVO();
            vo.setStudentId(sid);
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setRealName(u.getRealName());
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
                vo.setSubject(u.getSubject());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** 收集当前教师所带班级的（去重）学生 id 集合 */
    private List<Long> myStudentIds() {
        List<ClassInfo> classes = classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, UserContext.userId()));
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = classes.stream().map(ClassInfo::getId).collect(Collectors.toList());
        List<ClassStudent> relations = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .in(ClassStudent::getClassId, classIds));
        return relations.stream()
                .map(ClassStudent::getStudentId)
                .distinct()
                .collect(Collectors.toList());
    }
}
