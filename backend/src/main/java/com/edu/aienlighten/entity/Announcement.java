package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    /** 公告/活动/维护 */
    private String tag;
    /** 0普通 1置顶 */
    private Integer isTop;
    private LocalDateTime createdAt;
}
