package com.edu.aienlighten.vo;

import lombok.Data;

/** 看板汇总 VO */
@Data
public class DashboardSummaryVO {

    private Long totalUsers;
    /** 近 7 天活跃学习人数 */
    private Long weekActive;
    /** 课程完成率（%） */
    private Integer courseCompleteRate;
    /** 实验/闯关次数 */
    private Long experimentCount;
}
