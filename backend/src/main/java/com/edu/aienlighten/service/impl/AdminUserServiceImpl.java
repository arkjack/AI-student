package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.UserCreateDTO;
import com.edu.aienlighten.dto.UserUpdateDTO;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.ClassStudent;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.ClassStudentMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.service.AdminUserService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.vo.PageVO;
import com.edu.aienlighten.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ClassStudentMapper classStudentMapper;
    private final OperationLogService operationLogService;

    @Override
    public PageVO<UserVO> page(Integer role, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (role != null) {
            w.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getNickname, keyword));
        }
        w.orderByDesc(User::getId);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), w);
        List<UserVO> records = p.getRecords().stream().map(AdminUserServiceImpl::toVO).collect(Collectors.toList());
        fillClass(records);
        PageVO<UserVO> vo = new PageVO<>();
        vo.setTotal(p.getTotal());
        vo.setRecords(records);
        vo.setPage(p.getCurrent());
        vo.setSize(p.getSize());
        return vo;
    }

    /** 回填学生所在班级 id 与名称 */
    private void fillClass(List<UserVO> records) {
        List<Long> studentIds = records.stream()
                .filter(u -> u.getRole() != null && u.getRole() == 2)
                .map(UserVO::getId)
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return;
        }
        List<ClassStudent> relations = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .in(ClassStudent::getStudentId, studentIds));
        if (relations.isEmpty()) {
            return;
        }
        Map<Long, Long> stuClassMap = relations.stream()
                .collect(Collectors.toMap(ClassStudent::getStudentId, ClassStudent::getClassId, (a, b) -> a));
        List<Long> classIds = relations.stream().map(ClassStudent::getClassId).distinct().collect(Collectors.toList());
        Map<Long, String> classMap = classInfoMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getName, (a, b) -> a));
        for (UserVO vo : records) {
            Long classId = stuClassMap.get(vo.getId());
            if (classId != null) {
                vo.setClassId(classId);
                vo.setClassName(classMap.get(classId));
            }
        }
    }

    @Override
    @Transactional
    public UserVO create(UserCreateDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(ENCODER.encode(dto.getPassword()));
        u.setRealName(dto.getRealName());
        u.setNickname(dto.getNickname() == null ? dto.getRealName() : dto.getNickname());
        u.setAvatar("🦊");
        u.setRole(dto.getRole());
        u.setSubject(dto.getSubject());
        u.setStatus(1);
        userMapper.insert(u);

        // 学生且提供了班级：优先按 classId 加入，其次按名称匹配
        if (dto.getRole() != null && dto.getRole() == 2) {
            Long targetClassId = dto.getClassId();
            if (targetClassId == null && dto.getClassName() != null && !dto.getClassName().isBlank()) {
                List<ClassInfo> classes = classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                        .eq(ClassInfo::getName, dto.getClassName())
                        .orderByAsc(ClassInfo::getId));
                targetClassId = classes.isEmpty() ? null : classes.get(0).getId();
            }
            if (targetClassId != null) {
                ClassStudent cs = new ClassStudent();
                cs.setClassId(targetClassId);
                cs.setStudentId(u.getId());
                classStudentMapper.insert(cs);
            }
        }
        operationLogService.record("新增用户", "用户：" + dto.getUsername() + "，角色：" + dto.getRole());
        return toVO(u);
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        User u = requireUser(id);
        u.setRealName(dto.getRealName());
        u.setNickname(dto.getNickname());
        u.setSubject(dto.getSubject());
        // 密码非空则重置
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            u.setPassword(ENCODER.encode(dto.getPassword()));
        }
        userMapper.updateById(u);
        // 学生且提供了班级 id：调整班级归属
        if (u.getRole() != null && u.getRole() == 2 && dto.getClassId() != null) {
            classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getStudentId, id));
            ClassStudent cs = new ClassStudent();
            cs.setClassId(dto.getClassId());
            cs.setStudentId(id);
            classStudentMapper.insert(cs);
        }
        operationLogService.record("编辑用户", "用户 id=" + id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User u = requireUser(id);
        u.setStatus(status);
        userMapper.updateById(u);
        operationLogService.record("变更用户状态", "用户 id=" + id + "，状态：" + status);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User u = requireUser(id);
        if (u.getRole() != null && u.getRole() == 0) {
            throw new BizException("管理员账号不可删除");
        }
        userMapper.deleteById(id);
        classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, id));
        operationLogService.record("删除用户", "用户 id=" + id + "，用户名：" + u.getUsername());
    }

    private User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        return u;
    }

    private static UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        vo.setSubject(u.getSubject());
        vo.setStatus(u.getStatus());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }
}
