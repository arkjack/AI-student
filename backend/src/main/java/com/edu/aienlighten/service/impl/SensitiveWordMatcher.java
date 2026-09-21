package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.SensitiveWord;
import com.edu.aienlighten.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 敏感词匹配器 —— 词库的读取缓存与匹配逻辑都收在这一个类里。
 *
 * <p><b>为什么单独抽一个类</b>：匹配算法是后续最可能被替换的部分。调用方只依赖
 * {@link #match(String)} 这一个方法，将来把它换成字典树（Trie / DFA）或引入第三方词库时，
 * 只需改动本类内部，{@code ContentSafetyServiceImpl} 与 {@code AiServiceImpl} 一行都不用改。</p>
 *
 * <p><b>当前实现</b>：词表遍历 + {@code String.contains}。词库规模在百级时性能完全够用，
 * 复杂度为 O(词数 × 文本长度)；词库上千后应换成 Trie，那时复杂度降到 O(文本长度)。</p>
 *
 * <p><b>已知局限</b>：本阶段只做去首尾空白 + 英文转小写这层轻量归一化，
 * 「赌 博」「賭博」「dubo」这类绕过手法仍能穿过。完整的全角转半角、变体字映射、
 * 去零宽字符等归一化，以及 Trie 匹配，属于下一阶段的增强项。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordMatcher {

    /** 缓存有效期：多实例部署时保证词库变更最终一致，单实例下由管理端主动失效 */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final SensitiveWordMapper sensitiveWordMapper;

    /** 词条快照：预先把待匹配文本与词都转小写，避免每次匹配重复转换 */
    public record WordEntry(Long id, String word, String lower, int category, int level) {
    }

    private volatile List<WordEntry> cache;
    private volatile long loadedAt;

    /**
     * 匹配文本中命中的全部敏感词。
     *
     * @param text 待检测文本（输入原文或 AI 返回内容）
     * @return 命中的词条，未命中返回空列表
     */
    public List<WordEntry> match(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String target = normalizeLite(text);
        List<WordEntry> hits = new ArrayList<>();
        for (WordEntry entry : entries()) {
            if (target.contains(entry.lower())) {
                hits.add(entry);
            }
        }
        return hits;
    }

    /** 当前生效的词条数，用于管理端展示与健康检查 */
    public int cachedWordCount() {
        return entries().size();
    }

    /** 词库发生增删改后调用，让下一次匹配重新读库 */
    public void invalidate() {
        cache = null;
        loadedAt = 0L;
    }

    /** 轻量归一化：去首尾空白 + 英文转小写（中文不受影响） */
    private String normalizeLite(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private List<WordEntry> entries() {
        List<WordEntry> snapshot = cache;
        long now = System.currentTimeMillis();
        if (snapshot == null || now - loadedAt > CACHE_TTL_MS) {
            snapshot = load();
        }
        return snapshot;
    }

    private synchronized List<WordEntry> load() {
        // 双重检查：并发进来时避免重复读库
        List<WordEntry> snapshot = cache;
        long now = System.currentTimeMillis();
        if (snapshot != null && now - loadedAt <= CACHE_TTL_MS) {
            return snapshot;
        }
        List<SensitiveWord> rows;
        try {
            rows = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                    .eq(SensitiveWord::getEnabled, 1)
                    .orderByAsc(SensitiveWord::getId));
        } catch (Exception e) {
            // 读库失败不能让 AI 功能整体不可用：本次退化为「无词库」并保留旧快照
            log.error("敏感词库加载失败，本次调用将不启用敏感词匹配", e);
            return snapshot == null ? List.of() : snapshot;
        }
        List<WordEntry> loaded = new ArrayList<>(rows.size());
        for (SensitiveWord row : rows) {
            if (row.getWord() == null || row.getWord().isBlank()) {
                continue;
            }
            loaded.add(new WordEntry(
                    row.getId(),
                    row.getWord(),
                    normalizeLite(row.getWord()),
                    row.getCategory() == null ? 7 : row.getCategory(),
                    row.getLevel() == null ? 2 : row.getLevel()));
        }
        cache = List.copyOf(loaded);
        loadedAt = now;
        log.info("敏感词库已加载，生效词条数：{}", loaded.size());
        return cache;
    }
}
