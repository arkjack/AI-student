package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.ProjectSaveDTO;
import com.edu.aienlighten.entity.BlocklyProject;
import com.edu.aienlighten.mapper.BlocklyProjectMapper;
import com.edu.aienlighten.security.UserContext;
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
        p.setTitle(dto.getTitle());
        p.setBlocksJson(dto.getBlocksJson());
        p.setCodeText(dto.getCodeText());
        Integer status = dto.getStatus() == null ? 0 : dto.getStatus();
        p.setStatus(status);
        if (status == 1) {
            p.setSubmittedAt(LocalDateTime.now());
        }
        blocklyProjectMapper.insert(p);
        return p;
    }

    @Override
    public BlocklyProject submit(Long id) {
        BlocklyProject p = getOwnedProject(id);
        p.setStatus(1);
        p.setSubmittedAt(LocalDateTime.now());
        blocklyProjectMapper.updateById(p);
        return p;
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
