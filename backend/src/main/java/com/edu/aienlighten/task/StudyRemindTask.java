package com.edu.aienlighten.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.Notification;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.NotificationMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.service.ActivityService;
import com.edu.aienlighten.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 学习提醒定时任务 —— 个人中心「学习提醒」开关的真实后端能力。
 *
 * <p>开关语义（两级）：
 * <ul>
 *   <li>{@code user.notify_enabled} 是<b>总开关</b>：关闭后不再接收任何站内消息（含学习提醒）；</li>
 *   <li>{@code user.remind_enabled} 是<b>子开关</b>：在接收消息的前提下，是否额外接收超期未学习提醒。</li>
 * </ul>
 *
 * <p>判定规则：已开启学习提醒的学生，若连续 {@link #IDLE_DAYS} 天没有任何学习行为
 * （看课进度 / 闯关 / 提交作业 / 提交编程作品 / AI 写作），就推送一条站内提醒。
 *
 * <p>执行时间默认每天 20:00，可用配置覆盖。联调时想让它每 2 分钟跑一次，
 * 在 application-prod.yml 里加（注意这里不能写成 javadoc 里带星号斜杠的 cron 片段，会提前闭合注释）：
 * <pre>app:
 *   remind:
 *     cron: "0 0/2 * * * ?"</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRemindTask {

    /** 超过多少天没有学习行为就提醒 */
    private static final int IDLE_DAYS = 2;

    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.remind.cron:0 0 20 * * ?}")
    public void remindIdleStudents() {
        LocalDate today = LocalDate.now();
        List<User> students = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 2)            // 只提醒学生
                .eq(User::getStatus, 1)          // 只提醒启用中的账号
                .eq(User::getRemindEnabled, 1)); // 只提醒开启了学习提醒的学生

        int reminded = 0;
        for (User s : students) {
            LocalDate last = activityService.lastActivityDate(s.getId());
            long idleDays = last == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(last, today);
            if (idleDays < IDLE_DAYS) {
                continue;
            }
            // 同一学生每天最多一条（避免把 cron 调密后刷屏）
            Long todayCount = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, s.getId())
                    .eq(Notification::getType, "remind")
                    .ge(Notification::getCreatedAt, today.atStartOfDay()));
            if (todayCount != null && todayCount > 0) {
                continue;
            }
            String text = last == null
                    ? "你还没有开始学习哦，去课程中心挑一门喜欢的课吧～"
                    : "已经 " + idleDays + " 天没有学习记录了，回来继续你的 AI 探索之旅吧～";
            // push 内部还会再判一次「消息通知」总开关
            notificationService.push(s.getId(), "remind", "学习提醒", text, "/student/courses");
            reminded++;
        }
        log.info("[学习提醒] 检查 {} 名学生，发出 {} 条提醒（静默阈值 {} 天）", students.size(), reminded, IDLE_DAYS);
    }
}
