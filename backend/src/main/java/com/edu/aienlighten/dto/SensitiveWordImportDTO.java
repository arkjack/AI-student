package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 敏感词批量导入：一行一个词，统一套用同一分类与级别 */
@Data
public class SensitiveWordImportDTO {

    /** 多行文本，每行一个敏感词；空行与前后空白会被忽略 */
    @NotBlank(message = "导入内容不能为空")
    private String text;

    /** 1涉政 2色情 3暴力 4赌博毒品 5迷信诈骗 6广告导流 7其他，缺省 7 */
    private Integer category;

    /** 1提示 2替换 3拦截，缺省 2 */
    private Integer level;

    private String remark;
}
