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
    /** 1开 0关 */
    private Integer contentFilter;
}
