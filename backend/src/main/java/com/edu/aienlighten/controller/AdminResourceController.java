package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.AiConfigDTO;
import com.edu.aienlighten.dto.ScenePolicyDTO;
import com.edu.aienlighten.dto.TemplateEnabledDTO;
import com.edu.aienlighten.entity.AiScenePolicy;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminAiConfigService;
import com.edu.aienlighten.service.BlocklyTemplateService;
import com.edu.aienlighten.service.ContentSafetyService;
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
    private final ContentSafetyService contentSafetyService;

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

    /**
     * 三个 AI 实验场景的安全策略。
     *
     * <p>这是「实验资源」页三张实验卡片的数据源——此前页面上的开关是写死在前端的演示数据，
     * 改完刷新就还原；接到本接口后才是真实生效的配置。</p>
     */
    @GetMapping("/ai-scene-policy")
    @RequireRole({0})
    public Result<List<AiScenePolicy>> scenePolicies() {
        return Result.ok(contentSafetyService.allPolicies());
    }

    /** 保存场景策略（按 scene upsert） */
    @PutMapping("/ai-scene-policy")
    @RequireRole({0})
    public Result<Void> saveScenePolicies(@RequestBody List<ScenePolicyDTO> policies) {
        contentSafetyService.savePolicies(policies);
        return Result.ok();
    }
}
