package com.edu.aienlighten.service.impl;

import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.entity.AiConfigEntity;
import com.edu.aienlighten.mapper.AiConfigMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeepSeek（OpenAI 兼容 /v1/chat/completions）接入实现。
 *
 * <p>仅使用 JDK 内置 {@link HttpClient} + Spring Boot 自带 Jackson（{@link ObjectMapper}），无新增依赖。
 * 任何网络异常 / 超时 / 非 200 均降级返回预设文案，接口层不抛未处理异常。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    /** 单行表约定的主键 id=1 */
    private static final Long CONFIG_ID = 1L;

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_TIMEOUT_SEC = 60;
    private static final int DEFAULT_RATE_LIMIT_PER_MIN = 20;
    private static final int DEFAULT_CONTENT_FILTER = 1;

    /** 内容安全开启时追加到系统提示词的安全约束 */
    private static final String SAFETY_PROMPT =
            "你是面向中小学生的科普助手，回答要通俗、积极、安全，不要讨论不适合未成年人的话题";

    private static final String DEFAULT_FALLBACK = "AI 暂时开小差了，请稍后再试～";
    private static final String ANSWER_FALLBACK = "网络有点忙，先去看看课程吧，AI 老师稍后就来！";
    private static final String QUIZ_FALLBACK = "出题老师休息一下，请稍后再试～";
    private static final String SENSITIVE_FALLBACK = "这个话题不太适合小朋友呢，换个问题试试吧～";

    /** 内置简易敏感词表（命中后返回安全提示文案） */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "色情", "赌博", "暴力恐怖", "血腥", "毒品", "枪械", "迷信", "诈骗", "恐怖主义"
    );

    private final AiConfigMapper aiConfigMapper;
    private final ObjectMapper objectMapper;

    /** 按用户维度的每分钟限流器缓存 */
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return doChat(systemPrompt, userPrompt, DEFAULT_FALLBACK);
    }

    @Override
    public String writing(String topic, String style, int length) {
        String prompt = "你是中小学写作老师。请以「" + topic + "」为主题，运用「" + style
                + "」风格，写一篇约 " + length + " 字的作文。要求：条理清晰、语言生动、积极向上、"
                + "符合中小学生阅读水平，直接输出作文正文，不要额外解释。";
        return doChat(prompt, "", DEFAULT_FALLBACK);
    }

    @Override
    public String answer(String question) {
        String prompt = "你是中小学生科普答疑老师，请用通俗易懂的语言，耐心解答学生的问题。";
        return doChat(prompt, question, ANSWER_FALLBACK);
    }

    @Override
    public List<Map<String, Object>> genQuiz(int count) {
        return genQuiz(1, count);
    }

    @Override
    public List<Map<String, Object>> genQuiz(int level, int count) {
        int safeCount = Math.max(count, 1);
        String levelText = switch (level) {
            case 2 -> "进阶";
            case 3 -> "挑战";
            default -> "基础";
        };
        String prompt = "你是中小学知识闯关出题老师。请出 " + safeCount + " 道" + levelText
                + "难度、适合中小学阶段的选择题。只输出一个 JSON 数组，不要输出任何解释或多余文字。"
                + "数组元素格式为：{\"question\":\"题干\",\"options\":[\"选项A\",\"选项B\",\"选项C\",\"选项D\"],\"answer\":\"正确选项内容\"}。"
                + "请确保 answer 与 options 中某个选项完全一致。";
        String content = doChat(prompt, "", QUIZ_FALLBACK);
        List<Map<String, Object>> quiz = parseQuiz(content, safeCount);
        if (quiz == null || quiz.isEmpty()) {
            log.warn("AI 出题解析失败，回退内置题库。原始内容: {}", content);
            return builtinQuiz(safeCount);
        }
        return quiz;
    }

    /**
     * 核心调用链路：读配置 - 校验密钥 - 限流 - 拼装安全提示 - 调用 - 内容安全扫描。
     * 网络异常 / 超时 / 非 200 均捕获后降级返回 {@code fallback}。
     */
    private String doChat(String systemPrompt, String userPrompt, String fallback) {
        AiConfigEntity config = loadConfig();

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(1101, "AI 接口未配置，请联系管理员");
        }

        rateLimit(config);

        String safeSystemPrompt = appendSafety(config, systemPrompt);

        String text;
        try {
            text = callDeepSeek(config, safeSystemPrompt, userPrompt);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("DeepSeek 调用异常，触发降级。cause={}", e.toString());
            return fallback;
        }

        if (isContentFilterOn(config) && hitSensitive(text)) {
            return SENSITIVE_FALLBACK;
        }
        return text;
    }

    /** 读取 ai_config 单行记录；无记录或字段缺失时用默认值补齐 */
    private AiConfigEntity loadConfig() {
        AiConfigEntity config = aiConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new AiConfigEntity();
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            config.setBaseUrl(DEFAULT_BASE_URL);
        }
        if (config.getModel() == null || config.getModel().isBlank()) {
            config.setModel(DEFAULT_MODEL);
        }
        if (config.getTimeoutSec() == null || config.getTimeoutSec() <= 0) {
            config.setTimeoutSec(DEFAULT_TIMEOUT_SEC);
        }
        if (config.getRateLimitPerMin() == null || config.getRateLimitPerMin() <= 0) {
            config.setRateLimitPerMin(DEFAULT_RATE_LIMIT_PER_MIN);
        }
        if (config.getContentFilter() == null) {
            config.setContentFilter(DEFAULT_CONTENT_FILTER);
        }
        return config;
    }

    /** 按 userId 做每分钟限流，超限抛业务异常（走全局异常处理） */
    private void rateLimit(AiConfigEntity config) {
        Long userId = UserContext.userId();
        String key = userId == null ? "anonymous" : String.valueOf(userId);
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> new RateLimiter(config.getRateLimitPerMin()));
        if (!limiter.tryAcquire()) {
            throw new BizException(1102, "请求太频繁，休息一下再试吧～");
        }
    }

    /** 内容安全开启时，向系统提示词追加安全约束 */
    private String appendSafety(AiConfigEntity config, String systemPrompt) {
        if (isContentFilterOn(config)) {
            return systemPrompt + "\n" + SAFETY_PROMPT;
        }
        return systemPrompt;
    }

    private boolean isContentFilterOn(AiConfigEntity config) {
        return config.getContentFilter() != null && config.getContentFilter() == 1;
    }

    /** 调用 DeepSeek /v1/chat/completions，返回 choices[0].message.content；非 200 抛 IOException 触使降级 */
    private String callDeepSeek(AiConfigEntity config, String systemPrompt, String userPrompt)
            throws IOException, InterruptedException {
        int timeoutSec = config.getTimeoutSec() == null || config.getTimeoutSec() <= 0
                ? DEFAULT_TIMEOUT_SEC : config.getTimeoutSec();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .build();

        String url = normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions";
        String body = buildRequestBody(config.getModel(), systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("DeepSeek 返回非 200：status={}，body={}", resp.statusCode(), resp.body());
            throw new IOException("DeepSeek HTTP " + resp.statusCode());
        }
        return parseContent(resp.body());
    }

    /** 拼装请求体：{model, messages:[{role:system},{role:user}], temperature:0.7, max_tokens:800} */
    private String buildRequestBody(String model, String systemPrompt, String userPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);
        root.put("temperature", 0.7);
        root.put("max_tokens", 800);
        return objectMapper.writeValueAsString(root);
    }

    /** 解析响应 choices[0].message.content */
    private String parseContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IOException("DeepSeek 响应缺少 choices[0].message.content");
        }
        return content.asText();
    }

    /** 解析 AI 出题的 JSON 数组；解析失败返回 null（由调用方回退内置题库） */
    private List<Map<String, Object>> parseQuiz(String content, int count) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonArray(content));
            if (!root.isArray()) {
                return null;
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode question = item.path("question");
                JsonNode answer = item.path("answer");
                JsonNode options = item.path("options");
                if (question.isMissingNode() || answer.isMissingNode()
                        || !options.isArray() || options.isEmpty()) {
                    continue;
                }
                List<String> optionList = new ArrayList<>();
                for (JsonNode opt : options) {
                    optionList.add(opt.asText());
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("question", question.asText());
                map.put("options", optionList);
                map.put("answer", answer.asText());
                result.add(map);
                if (result.size() >= count) {
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            log.warn("AI 出题 JSON 解析失败", e);
            return null;
        }
    }

    /** 剔除 markdown 代码围栏，只保留 JSON 数组片段，便于稳健解析 */
    private String extractJsonArray(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        text = text.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /** 内置 5 题题库模板（出题解析失败时兜底） */
    private List<Map<String, Object>> builtinQuiz(int count) {
        List<Map<String, Object>> all = new ArrayList<>();
        all.add(quizItem("光在真空中的传播速度约为多少？",
                "每秒 30 万公里", "每秒 3 万公里", "每秒 3000 公里", "每秒 30 亿公里", "每秒 30 万公里"));
        all.add(quizItem("水在标准大气压下的沸点是多少？",
                "0℃", "50℃", "100℃", "200℃", "100℃"));
        all.add(quizItem("太阳系中体积最大的行星是哪一颗？",
                "地球", "火星", "木星", "土星", "木星"));
        all.add(quizItem("中国最长的河流是哪一条？",
                "黄河", "长江", "珠江", "淮河", "长江"));
        all.add(quizItem("下面哪一种动物属于两栖动物？",
                "青蛙", "乌龟", "鳄鱼", "金鱼", "青蛙"));
        int size = Math.max(0, Math.min(count, all.size()));
        return new ArrayList<>(all.subList(0, size));
    }

    private Map<String, Object> quizItem(String question, String a, String b, String c, String d, String answer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("question", question);
        map.put("options", Arrays.asList(a, b, c, d));
        map.put("answer", answer);
        return map;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /** 响应文本敏感词扫描 */
    private boolean hitSensitive(String text) {
        if (text == null) {
            return false;
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** 简约每分钟固定窗口限流器（按 userId 维度） */
    private static class RateLimiter {
        private final int limit;
        private final AtomicInteger count = new AtomicInteger();
        private volatile long windowStart;

        private RateLimiter(int limit) {
            this.limit = limit;
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000L) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
