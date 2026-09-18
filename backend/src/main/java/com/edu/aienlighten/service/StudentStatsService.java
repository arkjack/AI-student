package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.StudentStatsVO;

public interface StudentStatsService {

    /** 当前登录学生的学习数据聚合（含经验与等级） */
    StudentStatsVO myStats();
}
