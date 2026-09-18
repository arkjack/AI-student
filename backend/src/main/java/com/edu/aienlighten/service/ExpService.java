package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.ExpLogVO;

import java.util.List;

/**
 * 经验值服务。
 *
 * <p><b>设计原则</b>
 * <ol>
 *   <li><b>账本为唯一真相</b>：总经验 = exp_log 流水的 SUM，不再用「实时推导」算分，
 *       因此删掉作品、老师撤销批改都不会让经验倒退；</li>
 *   <li><b>幂等</b>：(studentId, sourceKey) 唯一，同一件事重复触发只记一次；</li>
 *   <li><b>服务端裁决</b>：所有加分都在后端业务动作里触发，不对外暴露「加分接口」；</li>
 *   <li><b>防刷</b>：单日经验上限 {@code ExpLevels.DAILY_CAP}。</li>
 * </ol>
 */
public interface ExpService {

    /* ================= 经验数值表（集中定义，方便调参） ================= */

    /** 每日首次学习 */
    int EXP_DAILY = 5;
    /** 看课到 25% / 50% */
    int EXP_COURSE_MILESTONE = 5;
    /** 看课到 75% */
    int EXP_COURSE_MILESTONE_LATE = 10;
    /** 完成课程（100%） */
    int EXP_COURSE_DONE = 50;
    /** 提交编程作品 */
    int EXP_PROJECT_SUBMIT = 20;
    /** 提交 AI 写作 */
    int EXP_WRITING_SUBMIT = 15;
    /** 完成一次知识闯关 */
    int EXP_QUIZ_DONE = 10;
    /** 闯关满分（额外） */
    int EXP_QUIZ_PERFECT = 15;
    /** 提交作业 */
    int EXP_SUBMISSION = 10;
    /** 作业被评 90 分以上（额外） */
    int EXP_SUBMISSION_EXCELLENT = 20;

    /**
     * 记一笔经验（幂等 + 单日上限）。
     *
     * @return true 表示本次真的入账；false 表示已记过 / 超出当日上限 / 入账失败
     */
    boolean award(Long studentId, String sourceType, String sourceKey, int exp, String remark);

    /**
     * 学习动作统一入口：每日首次学习 + 连续学习里程碑。
     * 由各个业务触发点在完成主要动作后调用，返回值是本次附加获得的经验（通常为 0）。
     */
    int onStudyAction(Long studentId);

    /** 学生累计总经验 */
    int totalExp(Long studentId);

    /** 最近经验流水（给学生看的「经验明细」） */
    List<ExpLogVO> recentLogs(Long studentId, int limit);
}
