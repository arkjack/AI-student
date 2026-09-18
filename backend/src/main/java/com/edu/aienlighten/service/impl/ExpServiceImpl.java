package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.ExpLevels;
import com.edu.aienlighten.entity.ExpLog;
import com.edu.aienlighten.mapper.ExpLogMapper;
import com.edu.aienlighten.service.ActivityService;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.vo.ExpLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpServiceImpl implements ExpService {

    /** 连续学习里程碑：{天数, 奖励经验}。每档终生只给一次 —— 断了再连也不重复发。 */
    private static final int[][] STREAK_MILESTONES = {
            {3, 30}, {7, 80}, {14, 200}, {30, 500}
    };

    private final ExpLogMapper expLogMapper;
    private final ActivityService activityService;

    @Override
    public boolean award(Long studentId, String sourceType, String sourceKey, int exp, String remark) {
        if (studentId == null || sourceKey == null || sourceKey.isBlank() || exp == 0) {
            return false;
        }
        // 幂等第一道：绝大多数重复触发在这里就被挡掉
        Long exists = expLogMapper.selectCount(new LambdaQueryWrapper<ExpLog>()
                .eq(ExpLog::getStudentId, studentId)
                .eq(ExpLog::getSourceKey, sourceKey));
        if (exists != null && exists > 0) {
            return false;
        }
        // 单日上限只约束「获得」，撤销（负数）不受限
        if (exp > 0 && todayExp(studentId) + exp > ExpLevels.DAILY_CAP) {
            return false;
        }

        ExpLog row = new ExpLog();
        row.setStudentId(studentId);
        row.setSourceType(sourceType);
        row.setSourceKey(sourceKey);
        row.setExp(exp);
        row.setRemark(remark);
        row.setCreatedAt(LocalDateTime.now());
        try {
            expLogMapper.insert(row);
            return true;
        } catch (Exception e) {
            // 幂等第二道：并发下由唯一键兜底。
            // MySQL/InnoDB 遇到重复键错误不会把整个事务置为失败，因此这里吞掉异常是安全的 ——
            // 最坏情况是这一笔经验没记上，而主业务（提交作品、批改作业等）照常完成。
            log.warn("经验入账失败 student={} key={} exp={} : {}", studentId, sourceKey, exp, e.getMessage());
            return false;
        }
    }

    @Override
    public int onStudyAction(Long studentId) {
        if (studentId == null) {
            return 0;
        }
        int gained = 0;

        // ① 每日首次学习
        String today = LocalDate.now().toString();
        if (award(studentId, "daily", "daily:" + today, EXP_DAILY, "每日首次学习")) {
            gained += EXP_DAILY;
        }

        // ② 连续学习里程碑（达到即发，重复调用由幂等键挡住）
        int streak = activityService.streakDays(studentId);
        for (int[] m : STREAK_MILESTONES) {
            if (streak >= m[0]
                    && award(studentId, "streak", "streak:" + m[0], m[1], "连续学习 " + m[0] + " 天")) {
                gained += m[1];
            }
        }
        return gained;
    }

    @Override
    public int totalExp(Long studentId) {
        if (studentId == null) {
            return 0;
        }
        return expLogMapper.selectList(new LambdaQueryWrapper<ExpLog>()
                        .eq(ExpLog::getStudentId, studentId))
                .stream()
                .mapToInt(l -> l.getExp() == null ? 0 : l.getExp())
                .sum();
    }

    @Override
    public List<ExpLogVO> recentLogs(Long studentId, int limit) {
        if (studentId == null) {
            return List.of();
        }
        int n = Math.min(Math.max(limit, 1), 100); // 夹紧，避免拼接进 SQL 的 LIMIT 被注入
        return expLogMapper.selectList(new LambdaQueryWrapper<ExpLog>()
                        .eq(ExpLog::getStudentId, studentId)
                        .orderByDesc(ExpLog::getCreatedAt)
                        .orderByDesc(ExpLog::getId)
                        .last("LIMIT " + n))
                .stream().map(l -> {
                    ExpLogVO vo = new ExpLogVO();
                    vo.setId(l.getId());
                    vo.setExp(l.getExp());
                    vo.setSourceType(l.getSourceType());
                    vo.setRemark(l.getRemark());
                    vo.setCreatedAt(l.getCreatedAt());
                    return vo;
                }).collect(Collectors.toList());
    }

    /** 今日已获得的正向经验，用于单日上限判断 */
    private int todayExp(Long studentId) {
        return expLogMapper.selectList(new LambdaQueryWrapper<ExpLog>()
                        .eq(ExpLog::getStudentId, studentId)
                        .ge(ExpLog::getCreatedAt, LocalDate.now().atStartOfDay())
                        .gt(ExpLog::getExp, 0))
                .stream()
                .mapToInt(l -> l.getExp() == null ? 0 : l.getExp())
                .sum();
    }
}
