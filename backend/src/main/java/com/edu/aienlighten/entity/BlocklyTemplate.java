package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 积木编程模板 */
@Data
@TableName("blockly_template")
public class BlocklyTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    /** 入门/进阶/挑战 */
    private String level;
    private String emoji;
    /** 1启用 0停用 */
    private Integer enabled;
    private LocalDateTime createdAt;
}
