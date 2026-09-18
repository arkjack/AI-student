package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.AiConfigDTO;
import com.edu.aienlighten.vo.AiConfigVO;

public interface AdminAiConfigService {

    /** 读取配置（apiKey 脱敏，只回显后 4 位） */
    AiConfigVO get();

    /** 保存配置（apiKey 为空串则保留原值） */
    void save(AiConfigDTO dto);
}
