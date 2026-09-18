package com.edu.aienlighten.common;

/**
 * 经验值与等级换算（纯函数，无状态）。
 *
 * <p><b>等级曲线</b>：从第 n 级升到 n+1 级需要
 * <pre>need(n) = 100 + 50 × (n - 1)</pre>
 * 即 100 / 150 / 200 / 250 … 递增，累计阈值为
 * <pre>total(n) = (n-1)×100 + 50×(n-1)×(n-2)/2</pre>
 *
 * <table>
 *   <tr><th>等级</th><th>本级所需</th><th>累计阈值</th></tr>
 *   <tr><td>Lv.1 → 2</td><td>100</td><td>0</td></tr>
 *   <tr><td>Lv.2 → 3</td><td>150</td><td>100</td></tr>
 *   <tr><td>Lv.3 → 4</td><td>200</td><td>250</td></tr>
 *   <tr><td>Lv.4 → 5</td><td>250</td><td>450</td></tr>
 *   <tr><td>Lv.5 → 6</td><td>300</td><td>700</td></tr>
 * </table>
 *
 * <p>之所以不用原先的 {@code level = exp/200 + 1}：那条线性曲线每级恒定 200 经验，
 * 升到 20 级与升到 2 级难度完全一样，后期毫无成长感。
 */
public final class ExpLevels {

    /** 1 级升 2 级所需经验 */
    public static final int BASE = 100;
    /** 每升一级，本级所需经验的增量 */
    public static final int STEP = 50;
    /** 单日经验上限，防止脚本刷分 */
    public static final int DAILY_CAP = 300;

    private ExpLevels() {
    }

    /** 从第 level 级升到 level+1 级所需经验 */
    public static int needFor(int level) {
        int lv = Math.max(1, level);
        return BASE + STEP * (lv - 1);
    }

    /** 达到第 level 级所需的累计经验（Lv.1 为 0） */
    public static int totalFor(int level) {
        int lv = Math.max(1, level);
        int n = lv - 1;
        return n * BASE + STEP * n * (n - 1) / 2;
    }

    /** 由累计经验反推当前等级 */
    public static int levelOf(int exp) {
        if (exp <= 0) {
            return 1;
        }
        int level = 1;
        // 单调递增，逐级试探即可；经验量级下循环次数约等于等级，开销可忽略
        while (totalFor(level + 1) <= exp) {
            level++;
        }
        return level;
    }

    /** 本级已获得的经验 */
    public static int expWithinLevel(int exp) {
        int level = levelOf(exp);
        return Math.max(0, exp - totalFor(level));
    }

    /** 本级升级所需经验 */
    public static int expNeedOfLevel(int exp) {
        return needFor(levelOf(exp));
    }

    /** 本级进度百分比 0~100（前端可直接画进度条） */
    public static int progressPercent(int exp) {
        int need = expNeedOfLevel(exp);
        if (need <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round(expWithinLevel(exp) * 100.0 / need));
    }

    /** 距离下一级还差多少经验 */
    public static int expToNextLevel(int exp) {
        return Math.max(0, expNeedOfLevel(exp) - expWithinLevel(exp));
    }

    /**
     * 等级称号。面向中小学生，称号比纯数字更有激励感。
     */
    public static String titleOf(int level) {
        if (level <= 2) return "AI 新芽";
        if (level <= 4) return "AI 学徒";
        if (level <= 6) return "积木达人";
        if (level <= 8) return "AI 探索者";
        if (level <= 10) return "AI 小专家";
        return "AI 大师";
    }

    /** 等级徽章 emoji（与称号一一对应） */
    public static String emojiOf(int level) {
        if (level <= 2) return "🌱";
        if (level <= 4) return "🔍";
        if (level <= 6) return "🧩";
        if (level <= 8) return "🚀";
        if (level <= 10) return "🎓";
        return "🏆";
    }
}
