package com.edu.aienlighten.vo;

import lombok.Data;

/** 积木编程模板 VO */
@Data
public class BlocklyTemplateVO {

    private Long id;
    private String name;
    private String description;
    private String emoji;
    /** 入门/进阶/挑战 */
    private String level;
    /** 1启用 0停用 */
    private Boolean enabled;
}
