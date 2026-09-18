package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.entity.BlocklyTemplate;
import com.edu.aienlighten.mapper.BlocklyTemplateMapper;
import com.edu.aienlighten.service.BlocklyTemplateService;
import com.edu.aienlighten.vo.BlocklyTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlocklyTemplateServiceImpl implements BlocklyTemplateService {

    private final BlocklyTemplateMapper templateMapper;

    @Override
    public List<BlocklyTemplateVO> listAll() {
        return templateMapper.selectList(new LambdaQueryWrapper<BlocklyTemplate>()
                        .orderByAsc(BlocklyTemplate::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<BlocklyTemplateVO> listEnabled() {
        return templateMapper.selectList(new LambdaQueryWrapper<BlocklyTemplate>()
                        .eq(BlocklyTemplate::getEnabled, 1)
                        .orderByAsc(BlocklyTemplate::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void setEnabled(Long id, Boolean enabled) {
        BlocklyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("模板不存在");
        }
        t.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        templateMapper.updateById(t);
    }

    @Override
    public BlocklyTemplateVO findEnabledByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        BlocklyTemplate t = templateMapper.selectOne(new LambdaQueryWrapper<BlocklyTemplate>()
                .eq(BlocklyTemplate::getName, name.trim())
                .last("limit 1"));
        if (t == null || t.getEnabled() == null || t.getEnabled() != 1) {
            return null;
        }
        return toVO(t);
    }

    private BlocklyTemplateVO toVO(BlocklyTemplate t) {
        BlocklyTemplateVO vo = new BlocklyTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setDescription(t.getDescription());
        vo.setEmoji(t.getEmoji());
        vo.setLevel(t.getLevel());
        vo.setEnabled(t.getEnabled() != null && t.getEnabled() == 1);
        return vo;
    }
}
