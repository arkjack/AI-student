package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.DashboardSummaryVO;
import com.edu.aienlighten.vo.RegisterTrendVO;
import com.edu.aienlighten.vo.RoleDistVO;

import java.util.List;

public interface AdminDashboardService {

    DashboardSummaryVO summary();

    List<RegisterTrendVO> registerTrend();

    List<RoleDistVO> roleDist();
}
