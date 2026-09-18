package com.edu.aienlighten.dto;

import lombok.Data;

/** AI 知识闯关出题入参 */
@Data
public class AiQuizDTO {

    /** 难度（1 基础 2 进阶 3 挑战），默认 1 */
    private Integer level;

    /** 出题数量，默认 5 */
    private Integer count;
}
