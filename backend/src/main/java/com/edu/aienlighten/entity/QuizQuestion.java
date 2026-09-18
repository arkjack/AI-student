package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("quiz_question")
public class QuizQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关卡名 */
    private String level;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    /** A/B/C/D */
    private String answer;
    private Integer sort;
}
