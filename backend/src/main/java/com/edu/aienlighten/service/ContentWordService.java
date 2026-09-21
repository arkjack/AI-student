package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.SensitiveWordDTO;
import com.edu.aienlighten.dto.SensitiveWordImportDTO;
import com.edu.aienlighten.entity.SensitiveWord;
import com.edu.aienlighten.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 敏感词库管理（管理端）。
 *
 * <p>任何增删改都会立即让 {@code SensitiveWordMatcher} 的缓存失效，
 * 保证「管理端改完，下一次 AI 调用就用新词表」。</p>
 */
public interface ContentWordService {

    PageVO<SensitiveWord> page(String keyword, Integer category, Integer enabled, Integer page, Integer size);

    SensitiveWord create(SensitiveWordDTO dto);

    SensitiveWord update(Long id, SensitiveWordDTO dto);

    void delete(Long id);

    void setEnabled(Long id, boolean enabled);

    /** 批量导入，返回本次实际新增的词条数（已存在的词会被跳过） */
    int importWords(SensitiveWordImportDTO dto);

    /** 分类统计：各分类的启用/停用词条数，供管理端概览展示 */
    List<Map<String, Object>> categoryStats();
}
