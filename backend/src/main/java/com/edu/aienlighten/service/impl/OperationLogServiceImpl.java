package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.aienlighten.entity.OperationLog;
import com.edu.aienlighten.mapper.OperationLogMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(String opType, String detail) {
        OperationLog log = new OperationLog();
        log.setOperatorId(UserContext.userId());
        log.setOperatorRole(UserContext.role());
        log.setOpType(opType);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    @Override
    public Page<OperationLog> page(Integer operator, long page, long size) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        if (operator != null) {
            w.eq(OperationLog::getOperatorId, operator);
        }
        w.orderByDesc(OperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(page, size), w);
    }
}
