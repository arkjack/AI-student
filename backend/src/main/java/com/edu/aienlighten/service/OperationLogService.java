package com.edu.aienlighten.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.aienlighten.entity.OperationLog;

public interface OperationLogService {

    /** 记录一条操作日志，operatorId/role 自动取当前登录用户，ip 置空 */
    void record(String opType, String detail);

    /** 分页查询操作日志 */
    Page<OperationLog> page(Integer operator, long page, long size);
}
