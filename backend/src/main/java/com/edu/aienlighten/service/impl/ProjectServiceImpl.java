package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.ProjectSaveDTO;
import com.edu.aienlighten.entity.BlocklyProject;
import com.edu.aienlighten.mapper.BlocklyProjectMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final BlocklyProjectMapper blocklyProjectMapper;
    private final ExpService expService;

    @Override
    public List<BlocklyProject> myProjects() {
        return blocklyProjectMapper.selectList(new LambdaQueryWrapper<BlocklyProject>()
                .eq(BlocklyProject::getStudentId, UserContext.userId())
                .orderByDesc(BlocklyProject::getCreatedAt));
    }

    @Override
    public BlocklyProject saveProject(ProjectSaveDTO dto) {
        BlocklyProject p = new BlocklyProject();
        p.setStudentId(UserContext.userId());
        p.setAssignmentId(dto.getAssignmentId());
        p.setTitle(dto.getTitle());
        p.setBlocksJson(dto.getBlocksJson());
        p.setCodeText(dto.getCodeText());
        Integer status = dto.getStatus() == null ? 0 : dto.getStatus();
        p.setStatus(status);
        if (status == 1) {
            p.setSubmittedAt(LocalDateTime.now());
        }
        blocklyProjectMapper.insert(p);
        if (status == 1) {
            awardProject(UserContext.userId(), p);
        }
        return p;
    }

    @Override
    public BlocklyProject submit(Long id) {
        BlocklyProject p = getOwnedProject(id);
        p.setStatus(1);
        p.setSubmittedAt(LocalDateTime.now());
        blocklyProjectMapper.updateById(p);
        awardProject(UserContext.userId(), p);
        return p;
    }

    /** 提交作品加经验。重复提交同一作品由 sourceKey 幂等挡住，不会反复给分。 */
    private void awardProject(Long studentId, BlocklyProject p) {
        expService.award(studentId, "project", "project:" + p.getId(),
                ExpService.EXP_PROJECT_SUBMIT,
                "提交编程作品「" + (p.getTitle() == null ? "未命名" : p.getTitle()) + "」");
        expService.onStudyAction(studentId);
    }

    @Override
    public void delete(Long id) {
        BlocklyProject p = getOwnedProject(id);
        blocklyProjectMapper.deleteById(p.getId());
    }

    private BlocklyProject getOwnedProject(Long id) {
        BlocklyProject p = blocklyProjectMapper.selectById(id);
        if (p == null) {
            throw new BizException(1007, "作品不存在");
        }
        if (!Objects.equals(p.getStudentId(), UserContext.userId())) {
            throw new BizException(403, "无权操作该作品");
        }
        return p;
    }
}
