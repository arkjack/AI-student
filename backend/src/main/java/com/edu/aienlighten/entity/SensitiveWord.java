package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词库。
 *
 * <p>{@code category} 不只是分类标签，同时是未成年人适宜性评估的权重来源；
 * {@code level} 决定命中后的处置动作（1 提示 2 替换 3 拦截）。</p>
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词 */
    private String word;

    /** 1涉政 2色情 3暴力 4赌博毒品 5迷信诈骗 6广告导流 7其他 */
    private Integer category;

    /** 1提示 2替换 3拦截 */
    private Integer level;

    /** 1启用 0停用 */
    private Integer enabled;

    private String remark;

    private LocalDateTime createdAt;
}
