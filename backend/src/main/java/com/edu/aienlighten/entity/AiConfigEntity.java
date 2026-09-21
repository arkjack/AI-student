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

    /** 模型名称，如 deepseek-flash（官方现行名）；旧名 deepseek-chat 已不再受支持 */
    private String model;

    /** 超时时间（秒） */
    private Integer timeoutSec;

    /** 最大并发数 */
    private Integer maxConcurrency;

    /** 每分钟限流次数 */
    private Integer rateLimitPerMin;

    /**
     * 内容安全运行模式：0 完全关闭 / 1 正常 / 2 观察模式。
     *
     * <p>刻意不做成简单开关：面向未成年人的平台不应该有「一键悄悄关掉所有保护」的入口。
     * 排查误杀请用观察模式——检测、打分、日志照常，只是不拦截。</p>
     */
    private Integer contentFilter;

    /** 运行模式取值 */
    public static final int MODE_OFF = 0;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_OBSERVE = 2;

    /** 越界或缺省一律按「正常」处理，避免脏数据把安全链路关掉 */
    public static int modeOf(Integer contentFilter) {
        if (contentFilter == null) {
            return MODE_NORMAL;
        }
        return switch (contentFilter) {
            case MODE_OFF -> MODE_OFF;
            case MODE_OBSERVE -> MODE_OBSERVE;
            default -> MODE_NORMAL;
        };
    }
}
