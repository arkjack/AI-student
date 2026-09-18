package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blockly_project")
public class BlocklyProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /**
     * 来源任务 id。
     * 非空 = 这是为某个教师布置的编程任务（assignment.type=2）提交的作品，老师会批改；
     * 为空 = 学生自己平时提交的作品，老师不批改。
     * 前端作品集据此决定显示「待批改 / XX 分」还是「已提交」。
     */
    private Long assignmentId;

    private String title;
    private String blocksJson;
    private String codeText;
    /** 0草稿 1已提交 */
    private Integer status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
