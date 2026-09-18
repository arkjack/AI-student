package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AiConfigDTO;
import com.edu.aienlighten.dto.TemplateEnabledDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminAiConfigService;
import com.edu.aienlighten.service.BlocklyTemplateService;
import com.edu.aienlighten.vo.AiConfigVO;
import com.edu.aienlighten.vo.BlocklyTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端 · 实验资源与 AI 配置 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResourceController {

    private final AdminAiConfigService adminAiConfigService;
    private final BlocklyTemplateService blocklyTemplateService;

    /** 编程模板列表（含启停状态） */
    @GetMapping("/templates")
    @RequireRole({0})
    public Result<List<BlocklyTemplateVO>> templates() {
        return Result.ok(blocklyTemplateService.listAll());
    }

    /** 启停模板 */
    @PutMapping("/templates/{id}/enabled")
    @RequireRole({0})
    public Result<Void> setTemplateEnabled(@PathVariable Long id, @RequestBody TemplateEnabledDTO dto) {
        blocklyTemplateService.setEnabled(id, dto.getEnabled());
        return Result.ok();
    }

    /** 读取 AI 配置（apiKey 脱敏） */
    @GetMapping("/ai-config")
    @RequireRole({0})
    public Result<AiConfigVO> aiConfig() {
        return Result.ok(adminAiConfigService.get());
    }

    /** 保存 AI 配置（apiKey 为空串保留原值） */
    @PutMapping("/ai-config")
    @RequireRole({0})
    public Result<Void> saveAiConfig(@RequestBody AiConfigDTO dto) {
        adminAiConfigService.save(dto);
        return Result.ok();
    }
}
