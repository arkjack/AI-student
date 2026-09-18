package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.QaAskDTO;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.QaRecord;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.QaRecordMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.QaService;
import com.edu.aienlighten.vo.QaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private final QaRecordMapper qaRecordMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final UserMapper userMapper;

    /** 学生提问：定向发送给负责自己的班级教师（待回复） */
    @Override
    public QaRecord ask(QaAskDTO dto) {
        QaRecord r = new QaRecord();
        r.setStudentId(UserContext.userId());
        r.setQuestion(dto.getQuestion());
        r.setAnswer(null);
        r.setAnswerType(2); // 提交给教师解答
        r.setTeacherId(findResponsibleTeacherId());
        qaRecordMapper.insert(r);
        return r;
    }

    /** 我的问答历史（近 50 条） */
    @Override
    public List<QaVO> myQuestions() {
        List<QaRecord> records = qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getStudentId, UserContext.userId())
                .orderByDesc(QaRecord::getId)
                .last("LIMIT 50"));
        if (records.isEmpty()) {
            return List.of();
        }
        // 组装教师姓名
        List<Long> teacherIds = records.stream()
                .map(QaRecord::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> teacherMap = teacherIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId,
                                u -> u.getRealName() != null ? u.getRealName() : u.getNickname(),
                                (a, b) -> a));
        return records.stream().map(r -> {
            QaVO vo = new QaVO();
            vo.setId(r.getId());
            vo.setStudentId(r.getStudentId());
            vo.setQuestion(r.getQuestion());
            vo.setAnswer(r.getAnswer());
            vo.setAnswerType(r.getAnswerType());
            vo.setTeacherId(r.getTeacherId());
            vo.setTeacherName(teacherMap.get(r.getTeacherId()));
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 找到学生所在班级的负责教师（class_info.teacher_id），取第一个班级 */
    private Long findResponsibleTeacherId() {
        Long sid = UserContext.userId();
        if (sid == null) {
            return null;
        }
        ClassStudent cs = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, sid)
                .last("LIMIT 1"));
        if (cs == null || cs.getClassId() == null) {
            return null;
        }
        ClassInfo ci = classInfoMapper.selectById(cs.getClassId());
        return ci == null ? null : ci.getTeacherId();
    }
}
