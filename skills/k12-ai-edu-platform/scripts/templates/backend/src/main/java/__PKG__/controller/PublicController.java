package __PKG__.controller;

import __PKG__.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开接口（无需登录）。
 *
 * <p>统一收敛到 {@code /api/public/**} 前缀，这样 WebConfig 的白名单只需要维护一条规则，
 * 不必每加一个公开接口就回去改配置。
 *
 * <p>⚠️ 放进这个包/前缀的接口等于对全世界开放，**不要**在这里返回任何用户数据。
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    /** 连通性自检：前端首页的「调用一次公开接口」按钮打这个 */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }
}
