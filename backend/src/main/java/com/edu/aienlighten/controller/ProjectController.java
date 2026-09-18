package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ProjectSaveDTO;
import com.edu.aienlighten.entity.BlocklyProject;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.BlocklyTemplateService;
import com.edu.aienlighten.service.ProjectService;
import com.edu.aienlighten.vo.BlocklyTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 积木编程作品模块（学生端） */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final BlocklyTemplateService blocklyTemplateService;

    /** 启用的积木模板（供学生/教师选择） */
    @GetMapping("/templates")
    @RequireRole({1, 2})
    public Result<List<BlocklyTemplateVO>> templates() {
        return Result.ok(blocklyTemplateService.listEnabled());
    }

    /** 我的编程作品列表 */
    @GetMapping("/my")
    @RequireRole({2})
    public Result<List<BlocklyProject>> my() {
        return Result.ok(projectService.myProjects());
    }

    /** 保存草稿/提交作品 */
    @PostMapping("/save")
    @RequireRole({2})
    public Result<BlocklyProject> save(@Valid @RequestBody ProjectSaveDTO dto) {
        return Result.ok(projectService.saveProject(dto));
    }

    /** 提交草稿 */
    @PostMapping("/{id}/submit")
    @RequireRole({2})
    public Result<BlocklyProject> submit(@PathVariable Long id) {
        return Result.ok(projectService.submit(id));
    }

    /** 删除作品（仅本人） */
    @DeleteMapping("/{id}")
    @RequireRole({2})
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }
}
