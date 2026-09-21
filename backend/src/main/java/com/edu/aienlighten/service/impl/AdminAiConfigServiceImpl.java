package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.AiConfigDTO;
import com.edu.aienlighten.entity.AiConfigEntity;
import com.edu.aienlighten.mapper.AiConfigMapper;
import com.edu.aienlighten.service.AdminAiConfigService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.vo.AiConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAiConfigServiceImpl implements AdminAiConfigService {

    private final AiConfigMapper aiConfigMapper;
    private final OperationLogService operationLogService;

    @Override
    public AiConfigVO get() {
        AiConfigEntity cfg = firstRow();
        if (cfg == null) {
            AiConfigVO def = new AiConfigVO();
            def.setProvider("deepseek");
            def.setApiKey(mask(null));
            def.setBaseUrl("https://api.deepseek.com/v1");
            def.setModel("deepseek-flash");
            def.setTimeoutSec(60);
            def.setMaxConcurrency(5);
            def.setRateLimitPerMin(20);
            def.setContentFilter(1);
            return def;
        }
        return toVO(cfg);
    }

    @Override
    public void save(AiConfigDTO dto) {
        AiConfigEntity cfg = firstRow();
        if (cfg == null) {
            cfg = new AiConfigEntity();
            cfg.setProvider("deepseek");
        }
        // apiKey 为空串表示不覆盖，保留原值
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            cfg.setApiKey(dto.getApiKey());
        }
        if (dto.getBaseUrl() != null) {
            cfg.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getModel() != null) {
            cfg.setModel(dto.getModel());
        }
        if (dto.getTimeoutSec() != null) {
            cfg.setTimeoutSec(dto.getTimeoutSec());
        }
        if (dto.getMaxConcurrency() != null) {
            cfg.setMaxConcurrency(dto.getMaxConcurrency());
        }
        if (dto.getRateLimitPerMin() != null) {
            cfg.setRateLimitPerMin(dto.getRateLimitPerMin());
        }
        if (dto.getContentFilter() != null) {
            // 运行模式只接受 0/1/2，越界值一律收敛为「正常」，避免脏数据把安全链路关掉
            cfg.setContentFilter(AiConfigEntity.modeOf(dto.getContentFilter()));
        }
        if (cfg.getId() == null) {
            // api_key 列为 NOT NULL 且无默认值：首次保存若未提供密钥，落空串占位
            if (cfg.getApiKey() == null) {
                cfg.setApiKey("");
            }
            aiConfigMapper.insert(cfg);
        } else {
            aiConfigMapper.updateById(cfg);
        }
        operationLogService.record("修改AI配置",
                "provider=" + (cfg.getProvider() == null ? "deepseek" : cfg.getProvider())
                        + "，内容安全模式=" + switch (AiConfigEntity.modeOf(cfg.getContentFilter())) {
                            case AiConfigEntity.MODE_OFF -> "完全关闭";
                            case AiConfigEntity.MODE_OBSERVE -> "观察模式";
                            default -> "正常";
                        });
    }

    private AiConfigEntity firstRow() {
        List<AiConfigEntity> list = aiConfigMapper.selectList(new LambdaQueryWrapper<AiConfigEntity>()
                .orderByAsc(AiConfigEntity::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    private AiConfigVO toVO(AiConfigEntity cfg) {
        AiConfigVO vo = new AiConfigVO();
        vo.setProvider(cfg.getProvider());
        vo.setApiKey(mask(cfg.getApiKey()));
        vo.setBaseUrl(cfg.getBaseUrl());
        vo.setModel(cfg.getModel());
        vo.setTimeoutSec(cfg.getTimeoutSec());
        vo.setMaxConcurrency(cfg.getMaxConcurrency());
        vo.setRateLimitPerMin(cfg.getRateLimitPerMin());
        vo.setContentFilter(cfg.getContentFilter());
        return vo;
    }

    /** 脱敏：只显示后 4 位 */
    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "****";
        }
        if (apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
