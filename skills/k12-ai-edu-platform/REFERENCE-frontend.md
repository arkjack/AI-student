# 前端骨架与约定

技术栈：Vue 3（组合式 API）+ Vite 6 + Element Plus + Vue Router + Pinia + ECharts + GSAP + Blockly + Phosphor Icons。

## 0. 依赖策略

脚手架 **只装最小可跑集合**，避免 `npm install` 拖入一堆用不到的包：

| 已内置 | 说明 |
|---|---|
| `vue` / `vue-router` / `pinia` / `axios` | 基础 |
| `element-plus` + `@element-plus/icons-vue` | UI 组件库 |
| `gsap` | `utils/motion.js` 需要 |
| `@fontsource/zcool-kuaile` | 站酷快乐体（标题字体） |

**阶段 6 做高交互页面时再按需安装**（这两个包体积大，提前装只会拖慢每次 install）：

```bash
npm i echarts blockly @phosphor-icons/vue
```

> ⚠️ `@fontsource/zcool-kuaile` 必须放在 `dependencies` 而不是 `devDependencies` —— 它是在 `main.js` 里**运行时 import** 的字体包，放错位置在部分部署流程下会缺失。


## 1. 目录结构

```
frontend/
├── index.html
├── vite.config.js
├── package.json
└── src/
    ├── main.js                  Element Plus 全量 + 中文 locale + 图标注册
    ├── App.vue
    ├── api/request.js           ★ 统一请求封装（唯一出口）
    ├── router/index.js          ★ 三端路由 + 角色守卫
    ├── styles/main.css          ★ 设计系统 Token（改视觉先看这里）
    ├── utils/motion.js          GSAP 统一工具（插件注册只在这里做一次）
    ├── components/              复用组件（CourseCard / NoticeBell ...）
    └── views/
        ├── Login.vue
        ├── student/             顶部导航布局 + 业务页
        ├── teacher/             侧栏布局 + 业务页
        ├── admin/               侧栏布局 + 业务页
        └── shared/              SidebarLayout / MessageCenter / Home
```

## 2. 请求封装（唯一出口）

模板：[`src/api/request.js`](scripts/templates/frontend/src/api/request.js)

```js
const body = res.data
if (body.code === 0) return body.data          // 成功直接解包出 data
if (body.code === 401) { clearAuth(); toLogin(); }   // 登录失效 → 踢回登录页
if (body.code === 403) { ElMessage.error(...) }      // 无权限 → 只提示，不动登录态
```

**两个不显眼但必须做的处理**：

1. **401 才清 token，403 仅提示**。早期把 403 也当登录失效处理，结果是管理员（角色 0）访问一个未放行的接口后被反复踢回登录页，排查了很久。
2. **登录失效提示要按时间窗口去重**。一次页面渲染可能并发多个受保护请求，全部 401 时会各弹一条「请先登录」，在屏幕上堆成一摞。做法是 3 秒窗口内只提示一次。

同时在 `main.js` 之外导出 `clearAuth()` 供退出登录复用，避免清 token 的逻辑散落各处。

## 3. 路由与守卫

模板：[`src/router/index.js`](scripts/templates/frontend/src/router/index.js)

角色编码与后端一致：**0 管理员 / 1 教师 / 2 学生**。守卫做三件事：

1. 未登录 → 转登录页并记录 `redirect`；
2. 已登录还访问 `/login`（含退出后手滑回退）→ 送自己角色的首页；
3. **角色越权**（学生直接敲 `/admin/dashboard`）→ 送回自己角色的首页。

> ⚠️ 前端守卫只是体验层拦截，**不能替代后端鉴权**。真正的权限判定在 `AuthInterceptor` + `@RequireRole`。

守卫里的角色→首页映射与登录页里的必须保持一致，建议抽成同一份常量，避免两处不同步。

## 4. 设计系统

模板：[`src/styles/main.css`](scripts/templates/frontend/src/styles/main.css)（全站唯一 Token 来源）

设计方向：**深海军蓝 + 暖橙单 accent、细边框取代软阴影、渐变清零**（去「AI 味」）。对标 Duolingo / Khan Kids 的活泼但克制。

| Token | 值 | 用途 |
|---|---|---|
| `--brand` | `#1e4fd8` | 主色（按钮 / 激活态 / 进度条） |
| `--brand-deep` | `#163a9e` | hover 深蓝 |
| `--navy` | `#15285c` | 深色面（Hero / 侧栏 / 登录品牌区） |
| `--accent` | `#ff8a3d` | **唯一**暖橙（CTA / 闯关 / 标签） |
| `--ink / --ink-2 / --ink-3` | 深→灰蓝 | 文字三层 |
| `--line` | `#e6eaf3` | 细边框 / 分隔线 |
| `--bg` | `#f8fafd` | 页面背景 |
| `--radius-lg/md/sm` | 16/12/10px | 卡片 / 控件 / 小元素 |

**硬性规则**：页面里禁止写死颜色与圆角，一律用 `var(--*)`。这样换主题只改一处。

基础类：`.k-page`（页容器）/ `.k-card`（细边框卡片）/ `.k-btn`（主蓝 / `--ghost` / `--orange`）/ `.k-tag`（含 `--orange/green/teal` 变体）/ `.page-head`（页头结构）。

## 5. 组件与实现约定

### 图标

```js
// ✅ 包的导出名是 PhXxx，必须用别名
import { PhHouse as House } from '@phosphor-icons/vue'
// ❌ import { House } from '@phosphor-icons/vue'   → build 直接失败
```

禁用 `el-icon`；emoji 只允许出现在头像、内容、徽章里，**不能当 UI 图标**。

### 动效

- GSAP 插件注册**只在 `src/utils/motion.js` 做一次**；
- 组件内用 `gsap.context(scope)`，并在 `onUnmounted` 里 `ctx.revert()`——否则热更新/切换路由会残留动画与监听；
- **所有动画包在 `matchMedia('(prefers-reduced-motion: no-preference)')` 内**（可访问性）。

### 进度条

```css
/* ✅ 只触发合成，不重排 */
transform: scaleX(0.6);
/* ❌ 触发布局重排，性能检测器会报警 */
width: 60%;
```

### Blockly（图形化编程）

- 解析 XML 用 `Blockly.utils.xml.textToDom(xml)` + `Blockly.Xml.domToWorkspace(dom, ws)`；
  **`Blockly.utils.xml.domToWorkspace` 不存在**，`DOMParser` 产出的 namespace 也不被 Blockly 识别；
- 生成的 JS 放进 **iframe 沙箱**执行，与主应用隔离；
- `text_print` 映射为 `console.log`，**不要用 `alert`**（阻塞式弹窗会卡死体验）；
- 模板启停要做三处拦截：下拉显示「（已停用）」、点击时弹提示并回退选择、`run` / `submit` 再拦一次。

### ECharts

图表容器要有明确高度；数据变化后用 `setOption(option, true)` 或按需 `resize`。横轴数据要确认字段名与后端返回一致（曾因读错字段名导致横轴恒为 `W1…W12`）。

## 6. Vite 配置

模板：[`vite.config.js`](scripts/templates/frontend/vite.config.js)，已配 `@` 别名与 `/api → 127.0.0.1:8080` 代理。

> ⚠️ **`build.emptyOutDir` 在 Windows + 中文路径下实测不生效**：`dist` 不会被清空，旧版本的按需分包会一直累积（曾累积到 1300 个文件、23 份 `Login-*.js`），造成 ①部署包逐次变大 ②已下线代码仍能通过 `/assets/<旧哈希>.js` 公开访问。
>
> **根因**：Vite 内部的 `emptyDir()` 用的是 `fs.rmSync(dir, {recursive:true, force:true})`，而该调用在此环境下会「不抛异常、也不删除任何东西」。
>
> **对策**：脚手架已内置 [`scripts/clean-dist.mjs`](scripts/templates/frontend/scripts/clean-dist.mjs)，改用逐文件 `unlinkSync` + 逐目录 `rmdirSync`（自底向上），并由 `package.json` 的 `prebuild` 钩子调用。**不要删掉这个脚本**，也不要改回依赖 `emptyOutDir`。

## 7. 验收三板斧

每次改动后依次执行：

```powershell
npm run build     # 构建必须通过
npm run dev       # 浏览器打开对应路由确认
# 以及项目自带的质量检测脚本（若有）
```

浏览器看到旧样式/旧逻辑时用 **Ctrl+Shift+R 强刷**；换浏览器或端口不符也会造成「改了没生效」的错觉。
