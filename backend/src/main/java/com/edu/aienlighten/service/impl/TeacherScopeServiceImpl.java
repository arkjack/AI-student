package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.TeacherScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherScopeServiceImpl implements TeacherScopeService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassStudentMapper classStudentMapper;

    @Override
    public List<Long> myStudentIds() {
        Long teacherId = UserContext.userId();
        if (teacherId == null) {
            return Collections.emptyList();
        }
        List<ClassInfo> classes = classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, teacherId));
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

    @Override
    public boolean covers(Long studentId) {
        return studentId != null && myStudentIds().contains(studentId);
    }
}
