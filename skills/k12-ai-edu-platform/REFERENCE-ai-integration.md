# 大模型接入工程化

**核心原则：把大模型当成一个随时会挂、会超时、会返回垃圾数据的不可靠外部依赖。**

只做到「能调通」是学生作业水平；做到「挂了也不影响平台运行」才是工程水平。下面五件事缺一不可。

## 1. 完整调用链路

```
读 ai_config 配置
  → 校验 apiKey 是否配置（没配 → 1101）
  → 按 userId 限流（超限 → 业务异常）
  → 拼装系统提示词（含面向未成年人的安全约束）
  → 调用 /v1/chat/completions
  → 内容安全扫描（命中敏感词 → 返回安全提示文案）
  → 返回结果
```

任何一个环节失败，都要落到「友好文案」而不是异常堆栈。

## 2. 配置落库 + 管理端可改

配置表 `ai_config` 字段：`provider / base_url / model / api_key / timeout_ms / rate_limit_per_min / safety_enabled`。

好处是**换模型不用改代码**。默认值：

```java
cfg.setProvider("deepseek");
cfg.setBaseUrl("https://api.deepseek.com/v1");   // OpenAI 兼容协议
cfg.setModel("deepseek-flash");                  // ⚠️ 见下方「模型名会变」
```

> **⚠️ 模型名会变，别写死旧名。**
> DeepSeek 现行模型名是 `deepseek-flash`（DeepSeek-V4.1-Flash）与 `deepseek-v4-pro`；
> 早期文档里的 `deepseek-chat` / `deepseek-reasoner` 已不在官方支持列表中。
> 模型名落在 `ai_config` 表里就是为了应对这种情况，但**代码里的默认值和 SQL 种子数据也要跟着改**，
> 否则新装的环境一上来就调不通。上线前扫一遍：
>
> ```bash
> grep -rn "deepseek-" --include=*.java --include=*.sql --include=*.vue .
> ```

调用时用 Java 内置 `HttpClient` 发 OpenAI 兼容请求即可，**不需要额外引 SDK**：

```java
POST {baseUrl}/chat/completions
Authorization: Bearer {apiKey}
{ "model": ..., "messages": [ {"role":"system","content":...}, {"role":"user","content":...} ] }

// 取 choices[0].message.content；非 200 一律抛 IOException 交给降级处理
```

### 思考模式：默认是开的，生成类场景要显式关掉

新模型（`deepseek-flash` / `deepseek-v4-pro`）**思考模式默认开启，且 effort 默认为 `high`**：
模型会先输出一大段思维链（`reasoning_content`）再给答案。对「写作文 / 科普答疑 / 出选择题」
这类简单生成任务，它带来三个实际危害：

1. **慢**：学生要等思维链跑完才能看到正文；
2. **贵**：思维链算输出 token，按输出价计费；
3. **可能把正文挤没**：思维链和正文共享 `max_tokens` 额度，上限设成 800 时正文可能为空——
   表现为「接口返回 200，但内容是空的」，很难排查。

而且思考模式下 `temperature` / `presence_penalty` / `frequency_penalty` **会被静默忽略**
（不报错但不生效），采样参数等于白设。

所以生成类请求应当显式关闭：

```java
root.put("temperature", 0.7);
root.put("max_tokens", 800);
root.putObject("thinking").put("type", "disabled");   // ← 关键
```

只有确实需要复杂推理的场景（数学解题、多步逻辑）才值得打开，那时再把 `max_tokens` 放大。

## 3. 限流（必做）

不做的后果：一个学生狂点按钮就能把配额刷光，全平台其他人当天都用不了。

按 `userId` 维度的每分钟固定窗口限流，实现只要几十行：

```java
/** 按用户维度的每分钟限流器缓存 */
private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

private void checkRateLimit(AiConfigEntity config, String userId) {
    RateLimiter limiter = limiters.computeIfAbsent(
            userId, k -> new RateLimiter(config.getRateLimitPerMin()));
    if (!limiter.tryAcquire()) {
        throw new BizException("问得太快啦，休息一下再试～");
    }
}

/** 简约每分钟固定窗口限流器 */
private static class RateLimiter {
    private final int limit;
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger count = new AtomicInteger();

    boolean tryAcquire() {
        long now = System.currentTimeMillis();
        if (now - windowStart.get() >= 60_000) {   // 进入新窗口
            windowStart.set(now);
            count.set(0);
        }
        return count.incrementAndGet() <= limit;
    }
}
```

> 用 `computeIfAbsent` 而不是先 `get` 再 `put`，避免并发下创建多个限流器导致限流失效。
> `limiters` 会随用户数增长，生产环境应加定期清理或改用带过期的缓存。

## 4. 内容安全（面向未成年人是硬要求）

两道防线：

**① 系统提示词层约束**

```java
private static final String SAFETY_PROMPT =
        "你是面向中小学生的科普助手，回答要通俗、积极、安全，不要讨论不适合未成年人的话题";
```

**② 返回内容扫描**

把敏感词表命中后的结果是**替换**，不是报错：

```java
private static final String SENSITIVE_FALLBACK = "这个话题不太适合小朋友呢，换个问题试试吧～";
```

`safety_enabled` 开关放在配置表里，便于按环境调整。

## 5. 三级降级（本方案的核心）

> 目标：**大模型完全不可用时，平台其他功能照常运行，AI 功能给出友好提示而不是报错或白屏。**

| 层级 | 失败场景 | 处理 |
|---|---|---|
| 一级 | 网络异常 / 超时 / 非 200 状态码 | 捕获后返回**场景化**预设文案 |
| 二级 | 出题场景返回内容解析不出来 | **回退内置题库**，功能不中断 |
| 三级 | 未配置 apiKey | 返回业务码 `1101`，前端提示去配置 |

预设文案要**按场景区分**，不要所有场景共用一句「服务繁忙」：

```java
private static final String DEFAULT_FALLBACK   = "AI 暂时开小差了，请稍后再试～";
private static final String ANSWER_FALLBACK    = "网络有点忙，先去看看课程吧，AI 老师稍后就来！";
private static final String QUIZ_FALLBACK      = "出题老师休息一下，请稍后再试～";
```

出题回退：

```java
String content = doChat(prompt, "", QUIZ_FALLBACK);
try {
    return parseQuiz(content);
} catch (Exception e) {
    log.warn("AI 出题解析失败，回退内置题库。原始内容: {}", content);
    return pickFromBuiltinBank(count, level);   // 内置题库兜底
}
```

**接口层永远不抛未处理异常**——`doChat` 内部把所有异常吃掉并转成 fallback 文案。

## 6. 密钥脱敏（必做）

API Key 只由后端持有，**任何查询接口返回前必须脱敏**：

```java
private String mask(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) return "";
    if (apiKey.length() <= 4) return "****";
    return "****" + apiKey.substring(apiKey.length() - 4);   // 只回显后 4 位
}
```

更新时：**传空串表示保留原值，不覆盖**。这样管理员在页面上看不到完整 Key，也无法把已有 Key 误清空。

## 7. Prompt 编排示例

三类场景的提示词模板（放在 service 里集中管理，不要散落在 Controller）：

```java
// 创意写作
String prompt = "你是中小学写作老师。请以「" + topic + "」为主题，运用「" + style + "」风格……";

// 科普答疑
String prompt = "你是中小学生科普答疑老师，请用通俗易懂的语言，耐心解答学生的问题。";

// 闯关出题：要求返回结构化格式，便于解析
String prompt = "你是中小学知识闯关出题老师。请出 " + count + " 道" + levelText + "……";
```

**出题类场景务必要求结构化输出**（固定字段的 JSON 或固定分隔符），否则解析失败率会很高——而这正是二级降级要兜住的。

## 8. 前端配合

- AI 接口要设置比普通接口更长的超时（生成类请求 30s 起）；
- 前端要有明确的 loading 态与「AI 正在思考…」提示，避免学生以为卡死反复点击（叠加后端限流会更快触发）；
- 收到 `1101` 时提示「AI 功能未配置，请联系管理员」，而不是「网络异常」。
