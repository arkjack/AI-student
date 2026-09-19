# 踩坑清单

以下每一条都是真实调试过的，按「现象 → 根因 → 对策」记录。**先读这份，能省掉大量返工。**

---

## 后端

### 1. MySQL 保留字导致建表/查询失败
- **现象**：`notification` 表的已读字段建表报语法错误。
- **根因**：`read` 是 MySQL 保留字。
- **对策**：字段名用 **`is_read`**。同类需警惕的还有 `order`、`status`、`desc`、`key`。

### 2. JDBC URL 里写了 utf8mb4 导致连接失败
- **对策**：`characterEncoding=UTF-8`，**不能写 `utf8mb4`**（JDBC 参数与 MySQL 字符集名不通用）。

### 3. PowerShell 执行 SQL 时 `$` 被展开
- **现象**：更新 BCrypt hash 的 SQL 执行后密码不对。
- **根因**：BCrypt hash 以 `$2a$10$...` 开头，PowerShell 双引号会把 `$2a` 当变量展开成空串。
- **对策**：把 SQL 写成文件再执行（`mysql < xx.sql`），或用单引号/转义。

### 4. AOP 合并切点绑定失败
- **现象**：`@Before("@annotation(r) || @within(r)")` 启动或运行时报参数绑定错误。
- **对策**：**方法级与类级分开定义两个切点**，各自 `@Before`，内部调用同一个 `check()`。见 REFERENCE-backend.md §2.3。

### 5. 401 与 403 混用会把有权限问题的用户反复踢出登录
- **现象**：管理员登录后弹「无权限访问」并被踢回登录页。
- **根因**：某接口未对管理员角色放行 → 403 → 前端把 403 也当登录失效处理 → 清 token。
- **对策**：① 拦截器区分「**401 才清 token，403 仅提示**」；② 复查所有被复用的布局件，其内部发起的请求必须按角色收敛；③ 若确实需要放宽，就把角色一并放行（如通知接口放行为 `{0,1,2}`）。

### 6. 调错接口路径被兜底成「系统繁忙」，误判为服务故障
- **根因**：`@ExceptionHandler(Exception.class)` 把 `NoResourceFoundException` 也吞了。
- **对策**：显式处理 `NoResourceFoundException`（→404）与 `HttpRequestMethodNotSupportedException`（→405）。

### 7. 生产环境 POST 接口 403、GET 却正常
- **根因**：CORS 未配置真实域名。浏览器对**同源 POST 也会带 `Origin` 头**，Spring 因此以 `403 Invalid CORS request` 拒绝。
- **对策**：部署后通过配置项补上真实域名，不要只盯着权限注解排查。

### 8. token 失效判定漏掉账号状态
- **根因**：只验签 + 验过期，已禁用/已删除账号手里的 token 仍有效到过期。
- **对策**：额外校验「账号存在且未禁用」+「密码未在签发后被修改」，注意 `iat` 只有秒级精度，比较要用**严格早于**（`iat >= pwdChangedAt`），否则改密同秒内重新登录的新 token 会被误判失效。

### 9. ThreadLocal 未清理导致用户串号
- **对策**：`UserContext.clear()` 必须放在拦截器的 `afterCompletion`，不能只放在 `preHandle` 成功分支。

### 10. 前后端字段类型不一致（联调期最耗时的坑）
- **现象**：页面显示 `1` 而不是「简单」，或筛选全部失效。
- **根因**：后端返回数字，前端按字符串/布尔判断（`isTop / status / difficulty`），或空值字段未处理（`resourceId / deadline`）。
- **对策**：mock 清零后用真实接口逐个页面过一遍；把类型约定写进接口文档。

---

## 前端

### 11. Phosphor 图标裸名导入直接 build 失败
- **对策**：`import { PhHouse as House } from '@phosphor-icons/vue'`——包的导出名统一带 `Ph` 前缀。

### 12. 进度条用 `width` 动画触发重排
- **对策**：一律 `transform: scaleX()`。性能检测器会直接报警。

### 13. GSAP 动画残留
- **对策**：插件注册只在 `utils/motion.js` 做一次；组件内 `gsap.context(scope)` + `onUnmounted → ctx.revert()`；所有动画包在 `matchMedia('(prefers-reduced-motion: no-preference)')` 内。

### 14. CSS 变量名写错会静默失效
- **现象**：某个颜色怎么改都没效果，也不报错。
- **根因**：引用了 `:root` 里根本不存在的 token（如 `--c-orange-deep`）。CSS 变量拼错不会报错，只会 fallback 到空值。
- **对策**：新增 token 才使用；改样式前先看 `main.css` 的 `:root`。

### 15. Blockly XML 解析 API 用错
- **对策**：`Blockly.utils.xml.textToDom(xml)` + `Blockly.Xml.domToWorkspace(dom, ws)`。
  **`Blockly.utils.xml.domToWorkspace` 不存在**；用 `DOMParser` 产出的 namespace 也不被识别。
  另外 `text_print` 要映射成 `console.log`，用 `alert` 会阻塞。

### 16. 前端并发 401 弹出一摞相同提示
- **对策**：登录失效提示按 3 秒窗口去重（见 REFERENCE-frontend.md §2）。

### 17. `vite build` 的 `emptyOutDir` 在 Windows + 中文路径下失效
- **现象**：`dist` 越积越大，曾累积到 1300 个文件、23 份 `Login-*.js`；**已下线代码仍能通过 `/assets/<旧哈希>.js` 公开访问**。
- **根因**：Vite 内部 `fs.existsSync(outDir)` 判定失败，即使显式设置 `emptyOutDir: true` 也不清空。
- **对策**：在 `package.json` 里加 `prebuild` 脚本真正负责删除目录，不要依赖 `emptyOutDir`。

### 18. 「改了没生效」的错觉
- **对策**：先 **Ctrl+Shift+R 强刷**；确认访问的端口与 `npm run dev` 一致；换浏览器测试。

---

## 环境与工具

### 19. PowerShell 发中文请求体乱码
- **对策**：把 JSON 写成 **UTF-8 无 BOM** 文件，再用 `curl.exe --data-binary "@file"` 发送。
  ```powershell
  [System.IO.File]::WriteAllText($p, $json, (New-Object System.Text.UTF8Encoding($false)))
  ```

### 20. Windows PowerShell 5.1 读 UTF-8 文件显示乱码
- **现象**：`Get-Content` 看到 `浠€涔堟槸`，误以为文件损坏。
- **根因**：PS 5.1 对无 BOM 的 UTF-8 文件默认按 GBK 解码。**文件本身是好的**。
- **对策**：用能识别 UTF-8 的工具读（编辑器 / 支持 `-Encoding utf8` 的命令），不要因此去"修复"文件而把内容真正弄坏。

### 20b. 含中文的 `.ps1` 脚本必须存成 UTF-8 **with BOM**
- **现象**：脚本报语法错误，错误位置指向一行普通的 `# 中文注释`，看不出任何语法问题。
- **根因**：PS 5.1 把无 BOM 的 `.ps1` 按 GBK 解码 → 中文注释变成乱码字节 → 其中恰好出现引号/反引号等元字符 → 解析器报错。**与 20 是同一个根因，但后果严重得多：20 只是显示问题，这条会让脚本直接跑不起来。**
- **对策**：写带中文的 `.ps1` 时显式写 BOM：
  ```powershell
  [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($true)))  # $true = 带 BOM
  ```
  纯 ASCII 脚本无所谓；**只要注释或输出里有中文就必须带 BOM**。

### 21. 中文路径下工具"假性找不到文件"
- **对策**：用「列出目录 → 按对象的 `FullName` 属性操作」的方式绕开，不要直接拼接中文路径字符串传给命令行工具。

### 22. `.gitignore` 规则没覆盖到变体，敏感/垃圾文件被重复提交
- **现象**：写好 `backend/sql/fix-*.sql` 后，`fix_password.sql` 仍每次被 `git add -A` 捞进仓库。
- **根因**：glob 里是**连字符** `fix-`，实际文件名是**下划线** `fix_`。
- **对策**：规则写宽一点（`fix*.sql`）；并且**验证而不是假设**：
  ```powershell
  git check-ignore -v --no-index <路径>   # 注意：默认不报告已跟踪文件，必须加 --no-index
  git add -A ; git status --short          # 再跑一次 add，看有没有被捞回来
  ```

### 23. 文件从仓库删除 ≠ 从历史删除
- **后果**：误提交过真密码后，`git rm` + commit 只是让最新版本没有它，**老版本永远留在 `.git` 里**；已 push 过的话远端历史里也有。
- **对策**：推送前扫描凭据；真误提交了要用 `git filter-repo` 重写历史 + 强推，已公开的还需联系平台清理。

---

## 流程与验收

### 24. mock 数据会掩盖真实的字段错误
- **根因**：前端用本地 mock 时，接口字段名写错、类型不匹配都不会报错——因为根本没走接口。
- **对策**：交付前必须清零 mock，逐个页面对真实接口。这个过程一定会暴露一批字段问题，属于正常成本，不是意外。

### 25. 演示数据要造齐关联关系
- **对策**：只插课程不插「班级-学生关联」，学生端任务、作业、进度全部为空，演示效果大打折扣。种子数据要覆盖：班级、学生入班、教师带班、课程进度、若干条作业与提交。
