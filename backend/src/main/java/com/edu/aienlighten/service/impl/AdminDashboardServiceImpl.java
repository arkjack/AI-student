package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.CourseProgressMapper;
import com.edu.aienlighten.mapper.QuizRecordMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.service.AdminDashboardService;
import com.edu.aienlighten.vo.DashboardSummaryVO;
import com.edu.aienlighten.vo.RegisterTrendVO;
import com.edu.aienlighten.vo.RoleDistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final CourseProgressMapper courseProgressMapper;
    private final QuizRecordMapper quizRecordMapper;

    @Override
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTotalUsers(userMapper.selectCount(null));

        // 近 7 天活跃学习人数
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<CourseProgress> recent = courseProgressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                .ge(CourseProgress::getUpdatedAt, weekAgo));
        long active = recent.stream().map(CourseProgress::getStudentId).distinct().count();
        vo.setWeekActive(active);

        // 课程完成率：course_progress.completed 均值（0/1）
        List<CourseProgress> all = courseProgressMapper.selectList(null);
        int rate = 0;
        if (!all.isEmpty()) {
            double avg = all.stream().mapToDouble(cp -> cp.getCompleted() == null ? 0 : cp.getCompleted()).average().orElse(0);
            rate = (int) Math.round(avg * 100);
        }
        vo.setCourseCompleteRate(rate);

        vo.setExperimentCount(quizRecordMapper.selectCount(null));
        return vo;
    }

    @Override
    public List<RegisterTrendVO> registerTrend() {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate firstMonday = thisMonday.minusWeeks(11);

        Map<LocalDate, Long> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            buckets.put(firstMonday.plusWeeks(i), 0L);
        }
        LocalDateTime cutoff = firstMonday.atStartOfDay();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, cutoff));
        for (User u : users) {
            if (u.getCreatedAt() == null) {
                continue;
            }
            LocalDate monday = u.getCreatedAt().toLocalDate().with(DayOfWeek.MONDAY);
            if (buckets.containsKey(monday)) {
                buckets.merge(monday, 1L, Long::sum);
            }
        }
        List<RegisterTrendVO> list = new ArrayList<>();
        buckets.forEach((monday, count) -> {
            RegisterTrendVO vo = new RegisterTrendVO();
            vo.setLabel(monday.toString());
            vo.setCount(count);
            list.add(vo);
        });
        return list;
    }

    @Override
    public List<RoleDistVO> roleDist() {
        List<User> all = userMapper.selectList(null);
        Map<Integer, Long> m = all.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        List<RoleDistVO> list = new ArrayList<>();
        list.add(make("管理员", m.getOrDefault(0, 0L)));
        list.add(make("教师", m.getOrDefault(1, 0L)));
        list.add(make("学生", m.getOrDefault(2, 0L)));
        return list;
    }

    private RoleDistVO make(String name, Long value) {
        RoleDistVO vo = new RoleDistVO();
        vo.setName(name);
        vo.setValue(value);
        return vo;
    }
}
