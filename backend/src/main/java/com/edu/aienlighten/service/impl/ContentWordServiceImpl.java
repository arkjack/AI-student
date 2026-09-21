package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.SensitiveWordDTO;
import com.edu.aienlighten.dto.SensitiveWordImportDTO;
import com.edu.aienlighten.entity.SensitiveWord;
import com.edu.aienlighten.mapper.SensitiveWordMapper;
import com.edu.aienlighten.service.ContentWordService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentWordServiceImpl implements ContentWordService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    /** 单次批量导入的词数上限，防止一次贴进来上万行把库写爆 */
    private static final int MAX_IMPORT = 500;

    private static final Map<Integer, String> CATEGORY_NAMES = Map.of(
            1, "涉政", 2, "色情", 3, "暴力", 4, "赌博毒品",
            5, "迷信诈骗", 6, "广告导流", 7, "其他");

    private final SensitiveWordMapper wordMapper;
    private final SensitiveWordMatcher matcher;
    private final OperationLogService operationLogService;

    @Override
    public PageVO<SensitiveWord> page(String keyword, Integer category, Integer enabled,
                                      Integer page, Integer size) {
        long current = page == null || page <= 0 ? 1 : page;
        long limit = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        LambdaQueryWrapper<SensitiveWord> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(SensitiveWord::getWord, keyword.trim());
        }
        if (category != null) {
            w.eq(SensitiveWord::getCategory, category);
        }
        if (enabled != null) {
            w.eq(SensitiveWord::getEnabled, enabled);
        }
        w.orderByAsc(SensitiveWord::getCategory).orderByDesc(SensitiveWord::getId);

        Page<SensitiveWord> p = wordMapper.selectPage(new Page<>(current, limit), w);
        PageVO<SensitiveWord> vo = new PageVO<>();
        vo.setTotal(p.getTotal());
        vo.setPage(p.getCurrent());
        vo.setSize(p.getSize());
        vo.setRecords(p.getRecords());
        return vo;
    }

    @Override
    public SensitiveWord create(SensitiveWordDTO dto) {
        String word = normalizeWord(dto.getWord());
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setCategory(dto.getCategory() == null ? 7 : dto.getCategory());
        entity.setLevel(dto.getLevel() == null ? 2 : dto.getLevel());
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setRemark(dto.getRemark());
        try {
            wordMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BizException("敏感词「" + word + "」已存在");
        }
        matcher.invalidate();
        operationLogService.record("新增敏感词", "词：" + word
                + "，分类：" + CATEGORY_NAMES.getOrDefault(entity.getCategory(), "其他"));
        return entity;
    }

    @Override
    public SensitiveWord update(Long id, SensitiveWordDTO dto) {
        SensitiveWord entity = wordMapper.selectById(id);
        if (entity == null) {
            throw new BizException("该敏感词不存在");
        }
        String word = normalizeWord(dto.getWord());
        entity.setWord(word);
        if (dto.getCategory() != null) {
            entity.setCategory(dto.getCategory());
        }
        if (dto.getLevel() != null) {
            entity.setLevel(dto.getLevel());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        entity.setRemark(dto.getRemark());
        try {
            wordMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BizException("敏感词「" + word + "」已存在");
        }
        matcher.invalidate();
        operationLogService.record("编辑敏感词", "词 id=" + id + "，词：" + word);
        return entity;
    }

    @Override
    public void delete(Long id) {
        SensitiveWord entity = wordMapper.selectById(id);
        if (entity == null) {
            throw new BizException("该敏感词不存在");
        }
        wordMapper.deleteById(id);
        matcher.invalidate();
        operationLogService.record("删除敏感词", "词：" + entity.getWord());
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        SensitiveWord entity = wordMapper.selectById(id);
        if (entity == null) {
            throw new BizException("该敏感词不存在");
        }
        entity.setEnabled(enabled ? 1 : 0);
        wordMapper.updateById(entity);
        matcher.invalidate();
    }

    @Override
    public int importWords(SensitiveWordImportDTO dto) {
        // 一行一个词；去空行、去前后空白、批内去重
        Set<String> candidates = new LinkedHashSet<>();
        for (String line : dto.getText().split("\\r?\\n")) {
            String w = line.trim();
            if (!w.isEmpty()) {
                candidates.add(w.length() > 50 ? w.substring(0, 50) : w);
            }
            if (candidates.size() >= MAX_IMPORT) {
                break;
            }
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        // 已存在的词直接跳过，避免整批因唯一键冲突而失败
        Set<String> existing = new LinkedHashSet<>();
        for (SensitiveWord row : wordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .in(SensitiveWord::getWord, new ArrayList<>(candidates)))) {
            existing.add(row.getWord());
        }

        int category = dto.getCategory() == null ? 7 : dto.getCategory();
        int level = dto.getLevel() == null ? 2 : dto.getLevel();
        int added = 0;
        for (String word : candidates) {
            if (existing.contains(word)) {
                continue;
            }
            SensitiveWord entity = new SensitiveWord();
            entity.setWord(word);
            entity.setCategory(category);
            entity.setLevel(level);
            entity.setEnabled(1);
            entity.setRemark(dto.getRemark());
            try {
                wordMapper.insert(entity);
                added++;
            } catch (DuplicateKeyException e) {
                log.debug("批量导入：词「{}」已存在，跳过", word);
            }
        }
        if (added > 0) {
            matcher.invalidate();
        }
        operationLogService.record("批量导入敏感词", "新增 " + added + " 条，分类："
                + CATEGORY_NAMES.getOrDefault(category, "其他"));
        return added;
    }

    @Override
    public List<Map<String, Object>> categoryStats() {
        List<SensitiveWord> all = wordMapper.selectList(new LambdaQueryWrapper<>());
        Map<Integer, long[]> counters = new LinkedHashMap<>();
        for (int c = 1; c <= 7; c++) {
            counters.put(c, new long[]{0, 0});
        }
        for (SensitiveWord w : all) {
            int c = w.getCategory() == null ? 7 : w.getCategory();
            long[] pair = counters.computeIfAbsent(c, k -> new long[]{0, 0});
            if (w.getEnabled() != null && w.getEnabled() == 1) {
                pair[0]++;
            } else {
                pair[1]++;
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : counters.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", e.getKey());
            item.put("name", CATEGORY_NAMES.getOrDefault(e.getKey(), "其他"));
            item.put("enabled", e.getValue()[0]);
            item.put("disabled", e.getValue()[1]);
            item.put("total", e.getValue()[0] + e.getValue()[1]);
            list.add(item);
        }
        return list;
    }

    private String normalizeWord(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("敏感词不能为空");
        }
        String w = raw.trim();
        if (w.length() > 50) {
            throw new BizException("敏感词长度不能超过 50");
        }
        return w;
    }
}
