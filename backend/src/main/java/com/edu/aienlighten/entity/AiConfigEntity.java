package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 配置（单行表：约定 id = 1）。
 * 密钥等敏感信息仅后端持有，由后端读取后调用 DeepSeek，不落库到响应。
 */
@Data
@TableName("ai_config")
public class AiConfigEntity {

    /** 主键，约定固定为 1 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务提供方，如 deepseek */
    private String provider;

    /** API 密钥（仅后端持有） */
    private String apiKey;

    /** 接口基础地址，如 https://api.deepseek.com/v1 */
    private String baseUrl;

    /** 模型名称，如 deepseek-chat */
    private String model;

    /** 超时时间（秒） */
    private Integer timeoutSec;

    /** 最大并发数 */
    private Integer maxConcurrency;

    /** 每分钟限流次数 */
    private Integer rateLimitPerMin;

    /** 内容安全过滤开关：1 开 0 关 */
    private Integer contentFilter;
}
