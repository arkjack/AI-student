package com.edu.aienlighten.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习行为（活跃度）计算。
 *
 * <p>单独抽出来是为了打破循环依赖：学习统计需要经验等级，经验服务需要算连续学习天数，
 * 两边都依赖本服务，而本服务只依赖数据表。
 *
 * <p>口径：看课进度更新 / 知识闯关 / 提交作业 / 提交编程作品 / AI 写作 —— 五类行为落到自然日。
 */
public interface ActivityService {

    /** 指定学生在时间窗内的全部学习行为日期（同一天多次会有重复项） */
    List<LocalDate> collectActivityDates(Long studentId, LocalDateTime startTime, LocalDateTime endTime);

    /** 最后一次学习行为的日期；从未学习过返回 null */
    LocalDate lastActivityDate(Long studentId);

    /** 连续学习天数：从今天（今天没学则从昨天）往前数连续有记录的天数 */
    int streakDays(Long studentId);
}
