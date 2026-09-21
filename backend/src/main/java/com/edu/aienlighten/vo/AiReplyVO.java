package com.edu.aienlighten.vo;

import lombok.Data;

/**
 * AI 调用返回体。
 *
 * <p>补上 {@code filtered} / {@code tip} 两个字段的原因：内容安全过滤命中时，后端返回的是
 * HTTP 200 + 安全文案，前端只靠 {@code content} 无法区分「这是一篇作文」还是
 * 「这是一句安全提示」。有了这两个字段，学生端就能把提示显示在提示条里，
 * 而不是把安全文案当成作文正文塞进编辑器。</p>
 */
@Data
public class AiReplyVO {

    /** 可直接展示给学生的内容：通过时为模型原文，被过滤时为安全文案 */
    private String content;

    /** 是否被内容安全机制处置过（替换或拦截） */
    private boolean filtered;

    /** 给学生看的提示语，仅 filtered=true 时有值 */
    private String tip;

    public static AiReplyVO of(String content) {
        AiReplyVO vo = new AiReplyVO();
        vo.content = content;
        vo.filtered = false;
        return vo;
    }

    public static AiReplyVO filtered(String content, String tip) {
        AiReplyVO vo = new AiReplyVO();
        vo.content = content;
        vo.filtered = true;
        vo.tip = tip;
        return vo;
    }
}
