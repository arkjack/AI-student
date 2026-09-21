package com.edu.aienlighten.dto;

import lombok.Data;

@Data
public class AiConfigDTO {

    /** apiKey 传空串表示保留原值，不覆盖 */
    private String apiKey;

    private String baseUrl;
    private String model;
    private Integer timeoutSec;
    private Integer maxConcurrency;
    private Integer rateLimitPerMin;
    /** 内容安全运行模式：0 完全关闭 / 1 正常 / 2 观察模式（只记录不拦截） */
    private Integer contentFilter;
}
