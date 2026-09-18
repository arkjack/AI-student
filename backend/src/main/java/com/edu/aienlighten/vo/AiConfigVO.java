package com.edu.aienlighten.vo;

import lombok.Data;

/** AI 配置 VO（apiKey 脱敏，只回显后 4 位） */
@Data
public class AiConfigVO {

    private String provider;
    /** 脱敏后的密钥：****xxx */
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer timeoutSec;
    private Integer maxConcurrency;
    private Integer rateLimitPerMin;
    /** 1开 0关 */
    private Integer contentFilter;
}
