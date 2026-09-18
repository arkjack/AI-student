package com.edu.aienlighten.vo;

import lombok.Data;

import java.util.List;

/** 学生端 · 学习数据聚合 VO */
@Data
public class StudentStatsVO {

    /** 已完成课程数 */
    private Integer completedCourses;
    /** 作品数量（已提交编程作品 + 已提交 AI 写作） */
    private Integer worksCount;
    /** 作业平均分（已批改作业得分均值） */
    private Integer avgScore;
    /** 闯关次数 */
    private Integer quizCount;
    /** 累计课程学习时长（分钟） */
    private Double studyMinutes;
    /** 已提交作业次数 */
    private Integer submitCount;

    /**
     * 连续学习天数（含今日）。
     * 口径：看课进度更新 / 闯关 / 提交作业 / 提交编程作品 / AI 写作 这几类行为落到自然日后去重，
     * 从今天往前数连续有记录的天数；今天还没有行为时从昨天起算。
     */
    private Integer streakDays;

    /** 最近 7 天学习动态（横轴标签，如 09-02） */
    private List<String> weekLabels;
    /** 最近 7 天学习动态（每日活跃次数） */
    private List<Integer> weekCounts;

    /** 成果分布（标签） */
    private List<String> distLabels;
    /** 成果分布（值） */
    private List<Integer> distValues;

    /** 作业得分趋势（横轴标签，如 09-03） */
    private List<String> scoreLabels;
    /** 作业得分趋势（分数） */
    private List<Integer> scoreValues;

    /** 成长等级 */
    private Integer level;
    /** 等级称号，如「AI 学徒」 */
    private String levelTitle;
    /** 等级徽章 emoji */
    private String levelEmoji;
    /** 累计总经验（来自 exp_log 流水求和） */
    private Integer exp;
    /** 本级已获得的经验 */
    private Integer levelExp;
    /** 本级升级所需经验 */
    private Integer levelExpNeed;
    /** 本级进度百分比 0~100，前端可直接画进度条 */
    private Integer levelProgress;
    /** 距离下一级还差多少经验 */
    private Integer nextLevelExp;

    /** 本月成就 */
    private List<MonthGoal> monthGoals;

    /** 本月成就单项 */
    @Data
    public static class MonthGoal {
        private String name;
        private Integer cur;
        private Integer goal;
    }
}
