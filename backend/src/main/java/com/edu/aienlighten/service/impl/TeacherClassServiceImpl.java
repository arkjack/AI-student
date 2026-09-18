package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.ClassCreateDTO;
import com.edu.aienlighten.entity.ClassApply;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassApplyMapper;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.service.TeacherClassService;
import com.edu.aienlighten.vo.ClassApplyVO;
import com.edu.aienlighten.vo.ClassStudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassStudentMapper classStudentMapper;
    private final UserMapper userMapper;
    private final ClassApplyMapper classApplyMapper;
    private final OperationLogService operationLogService;

    @Override
    public List<ClassInfo> listMyClasses() {
        Long teacherId = UserContext.userId();
        return classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, teacherId)
                .orderByDesc(ClassInfo::getId));
    }

    @Override
    public ClassInfo create(ClassCreateDTO dto) {
        ClassInfo c = new ClassInfo();
        c.setName(dto.getName());
        c.setGrade(dto.getGrade());
        c.setTeacherId(UserContext.userId());
        c.setDescription(dto.getDescription());
        classInfoMapper.insert(c);
        operationLogService.record("创建班级", "班级：" + dto.getName());
        return c;
    }

    @Override
    public void update(Long id, ClassCreateDTO dto) {
        ClassInfo c = requireOwned(id);
        c.setName(dto.getName());
        c.setGrade(dto.getGrade());
        c.setDescription(dto.getDescription());
        classInfoMapper.updateById(c);
        operationLogService.record("编辑班级", "班级：" + dto.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ClassInfo c = requireOwned(id);
        classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, id));
        classInfoMapper.deleteById(id);
        operationLogService.record("删除班级", "班级：" + c.getName());
    }

    @Override
    public List<ClassStudentVO> listStudents(Long classId) {
        requireOwned(classId);
        List<ClassStudent> relations = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .orderByAsc(ClassStudent::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = relations.stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return relations.stream().map(r -> {
            User u = userMap.get(r.getStudentId());
            ClassStudentVO vo = new ClassStudentVO();
            vo.setId(r.getId());
            vo.setStudentId(r.getStudentId());
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

    @Override
    public void addStudent(Long classId, Long studentId) {
        requireOwned(classId);
        Long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
        if (count > 0) {
            throw new BizException("该生已在班级");
        }
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        classStudentMapper.insert(cs);
        operationLogService.record("添加学生", "班级 id=" + classId + "，学生 id=" + studentId);
    }

    @Override
    public void removeStudent(Long classStudentId) {
        ClassStudent cs = classStudentMapper.selectById(classStudentId);
        if (cs == null) {
            throw new BizException("班级学生关联不存在");
        }
        requireOwned(cs.getClassId());
        classStudentMapper.deleteById(classStudentId);
        operationLogService.record("移出学生", "班级 id=" + cs.getClassId() + "，学生 id=" + cs.getStudentId());
    }

    @Override
    public List<ClassApplyVO> listApplies() {
        List<ClassInfo> myClasses = classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, UserContext.userId()));
        if (myClasses.isEmpty()) {
            return List.of();
        }
        List<Long> classIds = myClasses.stream().map(ClassInfo::getId).collect(Collectors.toList());
        Map<Long, String> classNameMap = myClasses.stream()
                .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getName, (a, b) -> a));
        List<ClassApply> applies = classApplyMapper.selectList(new LambdaQueryWrapper<ClassApply>()
                .in(ClassApply::getClassId, classIds)
                .orderByDesc(ClassApply::getId));
        if (applies.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = applies.stream().map(ClassApply::getStudentId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return applies.stream().map(a -> {
            User u = userMap.get(a.getStudentId());
            ClassApplyVO vo = new ClassApplyVO();
            vo.setId(a.getId());
            vo.setStudentId(a.getStudentId());
            if (u != null) {
                vo.setStudentName(u.getRealName());
                vo.setStudentNickname(u.getNickname());
            }
            vo.setClassId(a.getClassId());
            vo.setClassName(classNameMap.get(a.getClassId()));
            vo.setStatus(a.getStatus());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveApply(Long applyId) {
        ClassApply apply = classApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        requireOwned(apply.getClassId());
        if (apply.getStatus() != null && apply.getStatus() != 0) {
            throw new BizException("该申请已处理");
        }
        Long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, apply.getClassId())
                .eq(ClassStudent::getStudentId, apply.getStudentId()));
        if (count == 0) {
            ClassStudent cs = new ClassStudent();
            cs.setClassId(apply.getClassId());
            cs.setStudentId(apply.getStudentId());
            classStudentMapper.insert(cs);
        }
        apply.setStatus(1);
        apply.setReviewedAt(LocalDateTime.now());
        classApplyMapper.updateById(apply);
        operationLogService.record("审批入班", "申请 id=" + applyId + "，同意");
    }

    @Override
    public void rejectApply(Long applyId) {
        ClassApply apply = classApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        requireOwned(apply.getClassId());
        if (apply.getStatus() != null && apply.getStatus() != 0) {
            throw new BizException("该申请已处理");
        }
        apply.setStatus(2);
        apply.setReviewedAt(LocalDateTime.now());
        classApplyMapper.updateById(apply);
        operationLogService.record("审批入班", "申请 id=" + applyId + "，拒绝");
    }

    /** 校验班级存在且属于当前登录用户 */
    private ClassInfo requireOwned(Long classId) {
        ClassInfo c = classInfoMapper.selectById(classId);
        if (c == null) {
            throw new BizException("班级不存在");
        }
        if (!c.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("无权操作该班级");
        }
        return c;
    }
}
