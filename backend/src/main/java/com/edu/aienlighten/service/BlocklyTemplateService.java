package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.BlocklyTemplateVO;

import java.util.List;

public interface BlocklyTemplateService {

    /** 全部模板（管理端） */
    List<BlocklyTemplateVO> listAll();

    /** 仅启用模板（学生/教师端） */
    List<BlocklyTemplateVO> listEnabled();

    /** 设置模板启停 */
    void setEnabled(Long id, Boolean enabled);

    /** 按名称查找且已启用的模板 */
    BlocklyTemplateVO findEnabledByName(String name);
}
