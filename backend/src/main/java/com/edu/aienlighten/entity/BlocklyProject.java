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
