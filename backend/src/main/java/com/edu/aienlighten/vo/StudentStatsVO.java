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
    /** 当前经验 */
    private Integer exp;
    /** 升到下一级所需经验 */
    private Integer expNext;

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
