/* 全量功能测试：覆盖认证/权限/学生端/教师端/管理端/AI
   运行：node test-all.mjs
   后端需运行在 127.0.0.1:8080 */
const BASE = 'http://127.0.0.1:8080';

let pass = 0, fail = 0, skip = 0;
const failures = [];
const skips = [];
const summary = (d, key) => {
  try {
    if (Array.isArray(d)) return `${d.length} 项`;
    if (d && typeof d === 'object') {
      if (key && d[key] != null) return `${key}=${d[key]}`;
      if (d.total != null) return `total=${d.total}, 记录${d.records?.length ?? '?'}项`;
      return Object.keys(d).slice(0, 4).join(',');
    }
    return d;
  } catch { return ''; }
};

async function req(name, method, path, { token, body, expectCode = 0, dataKey, skipOn } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;
  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch (e) {
    log(name, false, '网络错误: ' + e.message);
    return null;
  }
  let j = null;
  try { j = await res.json(); } catch { j = { code: 'HTTP' + res.status, msg: await res.text() }; }
  const code = j?.code ?? 'HTTP' + res.status;
  // 依赖真实大模型返回的用例：未配置 API Key（1101）时记为「跳过」而不是失败，
  // 否则本地没配 Key 的环境会把环境问题误报成代码缺陷。
  if (skipOn !== undefined && code === skipOn) {
    skip++;
    skips.push(name);
    console.log(`⏭️  ${name}  →  跳过：code=${code} ${j?.msg ?? ''}`);
    return j;
  }
  const ok = code === expectCode;
  let detail = `code=${code}`;
  if (ok) detail += ' ' + (summary(j?.data, dataKey) || '');
  else detail += ` msg=${j?.msg ?? ''}`;
  log(name, ok, detail);
  return j;
}
function log(name, ok, detail) {
  const line = `${ok ? '✅' : '❌'} ${name}${detail ? '  →  ' + detail : ''}`;
  console.log(line);
  ok ? pass++ : (fail++, failures.push({ name, detail }));
}
const login = async (u, p) => {
  const j = await req(`登录 ${u}`, 'POST', '/api/auth/login', { body: { username: u, password: p } });
  return j?.code === 0 ? j.data.token : null;
};

// ============ 主流程 ============
console.log('========== ① 认证与权限 ==========');
const tAdmin = await login('admin', '123456');
const tTeacher = await login('teacher01', '123456');
const tStudent = await login('student01', '123456');
await req('错误密码登录(应1003)', 'POST', '/api/auth/login', { body: { username: 'student01', password: 'wrong' }, expectCode: 1003 });
await req('不存在用户登录', 'POST', '/api/auth/login', { body: { username: 'nobody99', password: '123456' }, expectCode: 1003 });
await req('空用户名登录(参数校验)', 'POST', '/api/auth/login', { body: { username: '', password: '123456' }, expectCode: 1002 });
// 注册
const regName = 'apitest' + Date.now().toString().slice(-6);
const regRes = await req('注册新学生', 'POST', '/api/auth/register', { body: { username: regName, password: '123456', nickname: '接口测试员' } });
const regId = regRes?.data?.id;
// 权限
await req('未登录访问受保护接口(应401)', 'GET', '/api/course/list', { expectCode: 401 });
await req('学生访问教师接口(应403)', 'GET', '/api/test/teacher', { token: tStudent, expectCode: 403 });
await req('学生访问管理员接口(应403)', 'GET', '/api/test/admin', { token: tStudent, expectCode: 403 });
await req('教师访问管理员接口(应403)', 'GET', '/api/test/admin', { token: tTeacher, expectCode: 403 });
await req('教师访问教师接口(应0)', 'GET', '/api/test/teacher', { token: tTeacher, expectCode: 0 });
await req('管理员访问管理员接口(应0)', 'GET', '/api/test/admin', { token: tAdmin, expectCode: 0 });
await req('获取当前用户 /user/me', 'GET', '/api/user/me', { token: tStudent, dataKey: 'username' });

console.log('\n========== ② 学生端（用户端） ==========');
const courses = await req('课程列表', 'GET', '/api/course/list', { token: tStudent });
const courseId = courses?.data?.[0]?.id ?? 1;
await req('课程详情', 'GET', `/api/course/${courseId}`, { token: tStudent });
await req('课程分类', 'GET', '/api/course/categories', { token: tStudent });
await req('课程列表(按难度筛选)', 'GET', '/api/course/list?difficulty=1', { token: tStudent });
await req('课程列表(关键词筛选)', 'GET', '/api/course/list?keyword=AI', { token: tStudent });

// 闯关
await req('闯关关卡列表', 'GET', '/api/quiz/levels', { token: tStudent });
await req('闯关题目(默认)', 'GET', '/api/quiz/questions', { token: tStudent });
await req('闯关题目(AI 基础)', 'GET', '/api/quiz/questions?level=AI%20基础', { token: tStudent });
await req('保存闯关成绩', 'POST', '/api/quiz/record', { token: tStudent, body: { level: 'AI基础', score: 80, correctCount: 4, totalCount: 5 } });

// 进度
await req('我的学习进度', 'GET', '/api/progress/my', { token: tStudent });
await req('保存学习进度', 'POST', '/api/progress/save', { token: tStudent, body: { courseId, progress: 50, watchSeconds: 120 } });

// 编程作品
await req('我的编程作品', 'GET', '/api/project/my', { token: tStudent });
const projRes = await req('保存编程作品', 'POST', '/api/project/save', { token: tStudent, body: { title: '接口测试作品', blocksJson: '[{"type":"move"}]', status: 0 } });
const projId = projRes?.data?.id;
if (projId) {
  await req('提交编程作品', 'POST', `/api/project/${projId}/submit`, { token: tStudent });
  await req('删除编程作品', 'DELETE', `/api/project/${projId}`, { token: tStudent });
}

// 写作
await req('我的写作记录', 'GET', '/api/writing/my', { token: tStudent });
await req('保存写作', 'POST', '/api/writing/save', { token: tStudent, body: { topic: '春天', style: '童话', content: '春天来了，花儿开了。', status: 1 } });

// 答疑
await req('我的问答历史', 'GET', '/api/qa/my', { token: tStudent });
const qaRes = await req('学生提问', 'POST', '/api/qa/ask', { token: tStudent, body: { question: '什么是人工智能？' } });
const qaId = qaRes?.data?.id;

// 作业（闭环：提交→批改→反馈）
const hwList = await req('我的作业任务', 'GET', '/api/homework/assignments', { token: tStudent });
const hwAssign = hwList?.data?.[0]?.id ?? 1;
await req('我的提交记录', 'GET', '/api/homework/submissions', { token: tStudent });
await req('提交作业', 'POST', '/api/homework/submit', { token: tStudent, body: { assignmentId: hwAssign, content: '人工智能能帮我们：1.查资料 2.画画 3.下棋。' } });
await req('查看批改反馈', 'GET', `/api/homework/feedback?assignmentId=${hwAssign}`, { token: tStudent });

// 公告
await req('公告列表(学生端)', 'GET', '/api/announcement/list', { token: tStudent });

console.log('\n========== ③ 教师端 ==========');
await req('我的班级列表', 'GET', '/api/teacher/classes', { token: tTeacher });
const clsRes = await req('创建班级', 'POST', '/api/teacher/classes', { token: tTeacher, body: { name: '接口测试班', grade: '四年级', description: '测试用' } });
const clsId = clsRes?.data?.id;
if (clsId) {
  await req('编辑班级', 'PUT', `/api/teacher/classes/${clsId}`, { token: tTeacher, body: { name: '接口测试班(改)', grade: '四年级' } });
  await req('班级学生列表', 'GET', `/api/teacher/classes/${clsId}/students`, { token: tTeacher });
  await req('添加学生到班级', 'POST', `/api/teacher/classes/${clsId}/students`, { token: tTeacher, body: { studentId: 4 } });
  const stuList = await req('再次查询班级学生', 'GET', `/api/teacher/classes/${clsId}/students`, { token: tTeacher });
  const csId = stuList?.data?.find(x => x.studentId === 4)?.id;
  if (csId) await req('移出班级学生', 'DELETE', `/api/teacher/classes/stu/${csId}`, { token: tTeacher });
}
// 任务
await req('我的任务列表', 'GET', '/api/teacher/assignments', { token: tTeacher });
const asgRes = await req('布置任务(多班级)', 'POST', '/api/teacher/assignments', { token: tTeacher, body: { title: '接口测试任务', classIds: clsId ? [1, clsId] : [1], type: 2, content: '测试', deadline: '2026-09-20 20:00' } });
if (clsId) {
  const mineAsg = await req('查询任务列表(含测试任务)', 'GET', '/api/teacher/assignments', { token: tTeacher });
  const testAsg = mineAsg?.data?.find(a => a.title === '接口测试任务');
  if (testAsg) {
    await req('截止任务', 'PUT', `/api/teacher/assignments/${testAsg.id}/close`, { token: tTeacher });
    await req('删除任务', 'DELETE', `/api/teacher/assignments/${testAsg.id}`, { token: tTeacher });
  }
}
// 进度
await req('班级学习概况', 'GET', '/api/teacher/progress/students?classId=1', { token: tTeacher });
await req('任务提交情况', 'GET', '/api/teacher/progress/assignment/1', { token: tTeacher });
// 批改（针对 student01 刚提交的作业）
const reviewList = await req('提交列表', 'GET', '/api/teacher/review/list?assignmentId=1', { token: tTeacher });
const subId = reviewList?.data?.find(s => s.studentId === 3)?.submissionId;
if (subId) {
  await req('批改作业', 'POST', `/api/teacher/review/${subId}`, { token: tTeacher, body: { score: 95, feedback: '回答很全面，继续加油！' } });
}
await req('待批改数', 'GET', '/api/teacher/review/pending-count', { token: tTeacher });
// 答疑
await req('学生提问列表', 'GET', '/api/teacher/qa/list', { token: tTeacher });
if (qaId) {
  await req('回复学生提问', 'POST', `/api/teacher/qa/${qaId}/reply`, { token: tTeacher, body: { answer: '人工智能是让机器学会像人一样思考的技术。' } });
}
await req('我班学生列表', 'GET', '/api/teacher/qa/students', { token: tTeacher });
// 公告（教师端统一）
await req('公告列表(教师端)', 'GET', '/api/announcement/list', { token: tTeacher });
// 删除测试班级
if (clsId) await req('删除测试班级', 'DELETE', `/api/teacher/classes/${clsId}`, { token: tTeacher });

console.log('\n========== ④ 管理端 ==========');
await req('看板汇总', 'GET', '/api/admin/dashboard/summary', { token: tAdmin });
await req('注册趋势', 'GET', '/api/admin/dashboard/register-trend', { token: tAdmin });
await req('角色分布', 'GET', '/api/admin/dashboard/role-dist', { token: tAdmin });
await req('用户分页列表', 'GET', '/api/admin/users?page=1&size=10', { token: tAdmin });
await req('用户列表(按角色筛选)', 'GET', '/api/admin/users?role=2', { token: tAdmin });
await req('用户列表(关键词搜索)', 'GET', '/api/admin/users?keyword=student', { token: tAdmin });
// 用户增删改
const uRes = await req('新增用户(学生)', 'POST', '/api/admin/users', { token: tAdmin, body: { role: 2, username: 'admintest' + Date.now().toString().slice(-6), realName: '测试学生', nickname: '测试昵称', password: '123456', className: '' } });
const newUid = uRes?.data?.id;
if (newUid) {
  await req('编辑用户', 'PUT', `/api/admin/users/${newUid}`, { token: tAdmin, body: { realName: '测试学生改', nickname: '测试昵称改' } });
  await req('禁用用户', 'PUT', `/api/admin/users/${newUid}/status`, { token: tAdmin, body: { status: 0 } });
  await req('启用用户', 'PUT', `/api/admin/users/${newUid}/status`, { token: tAdmin, body: { status: 1 } });
  await req('删除用户', 'DELETE', `/api/admin/users/${newUid}`, { token: tAdmin });
}
await req('删除管理员自身(应被拒)', 'DELETE', '/api/admin/users/1', { token: tAdmin, expectCode: 1001 });
// 课程
await req('课程列表(管理端)', 'GET', '/api/admin/courses', { token: tAdmin });
const cRes = await req('新增课程', 'POST', '/api/admin/courses', { token: tAdmin, body: { title: '接口测试课程', categoryId: 1, difficulty: 1, durationMinutes: 30, summary: '测试', status: 1 } });
const cId = cRes?.data?.id;
if (cId) {
  await req('编辑课程', 'PUT', `/api/admin/courses/${cId}`, { token: tAdmin, body: { title: '接口测试课程改', categoryId: 1, difficulty: 2, durationMinutes: 40, summary: '测试改', status: 1 } });
  await req('删除课程', 'DELETE', `/api/admin/courses/${cId}`, { token: tAdmin });
}
// 公告
await req('公告列表(管理端)', 'GET', '/api/admin/announcements', { token: tAdmin });
const aRes = await req('发布公告', 'POST', '/api/admin/announcements', { token: tAdmin, body: { title: '接口测试公告', content: '测试内容', tag: '公告', isTop: 0 } });
const aId = aRes?.data?.id ?? (await (async () => { const r = await fetch(BASE + '/api/admin/announcements', { headers: { Authorization: `Bearer ${tAdmin}` } }); const j = await r.json(); return j.data.find(x => x.title === '接口测试公告')?.id; })());
if (aId) await req('删除公告', 'DELETE', `/api/admin/announcements/${aId}`, { token: tAdmin });
// 日志/资源/配置
await req('操作日志列表', 'GET', '/api/admin/logs?page=1&size=10', { token: tAdmin });
await req('编程模板列表', 'GET', '/api/admin/templates', { token: tAdmin });
await req('AI 配置(脱敏)', 'GET', '/api/admin/ai-config', { token: tAdmin });
const aiCfg = await req('保存 AI 配置(空key保留原值)', 'PUT', '/api/admin/ai-config', { token: tAdmin, body: { apiKey: '', baseUrl: 'https://api.deepseek.com', model: 'deepseek-flash', timeoutSec: 60, maxConcurrency: 3, rateLimitPerMin: 20, contentFilter: 1 } });

console.log('\n========== ⑤ AI 能力 ==========');
// 这三项需要真实调用 DeepSeek：未配置 API Key 时记为跳过（在管理端「实验资源」页填 Key 后可完整跑通）
await req('AI 创意写作', 'POST', '/api/ai/writing', { token: tStudent, body: { topic: '会飞的小狗', style: '童话', length: 200 }, skipOn: 1101 });
await req('AI 智能答疑', 'POST', '/api/ai/chat', { token: tStudent, body: { question: '为什么天空是蓝色的？' }, skipOn: 1101 });
await req('AI 知识闯关出题', 'POST', '/api/ai/quiz', { token: tTeacher, body: { level: 1, count: 3 }, skipOn: 1101 });

console.log('\n========== ⑥ 内容安全（输入检测 / 敏感词 / 日志 / 复核） ==========');
// 前置条件：必须先执行 backend/sql/content-safety.sql 导入词库，
// 否则 S-01 会因为词库为空而「拦不住」，那是数据没初始化，不是代码缺陷。

/** 数据断言：req 只校验业务码，这里进一步校验返回体的具体字段 */
function check(name, cond, detail, skipped) {
  if (skipped) {
    skip++;
    skips.push(name);
    console.log(`⏭️  ${name}  →  跳过：${detail || 'AI 接口未配置'}`);
    return;
  }
  log(name, !!cond, detail);
}

/** 静默清理：不占用测试计数，失败也不影响结论 */
async function quiet(method, path, token, body) {
  try {
    await fetch(BASE + path, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        Authorization: `Bearer ${token}`
      },
      body: body ? JSON.stringify(body) : undefined
    });
  } catch (e) { /* 清理失败可忽略 */ }
}

const ts = Date.now().toString().slice(-5);

// ---------- 词库管理 ----------
await req('敏感词库分页查询', 'GET', '/api/admin/content/words?page=1&size=10', { token: tAdmin });
await req('词库分类统计', 'GET', '/api/admin/content/words/stats', { token: tAdmin });
const wRes = await req('新增敏感词', 'POST', '/api/admin/content/words', {
  token: tAdmin,
  body: { word: '接口测试词' + ts, category: 7, level: 2, enabled: 1, remark: '自动化测试' }
});
const wId = wRes?.data?.id;
if (wId) {
  await req('编辑敏感词', 'PUT', `/api/admin/content/words/${wId}`, {
    token: tAdmin,
    body: { word: '接口测试词' + ts, category: 7, level: 3, enabled: 1, remark: '自动化测试-已改' }
  });
  await req('停用敏感词', 'PUT', `/api/admin/content/words/${wId}/enabled`, { token: tAdmin, body: { enabled: false } });
  await req('删除敏感词', 'DELETE', `/api/admin/content/words/${wId}`, { token: tAdmin });
}
await req('重复新增已存在的词(应被拒)', 'POST', '/api/admin/content/words', {
  token: tAdmin, body: { word: '赌博', category: 4, level: 3 }, expectCode: 1001
});
const impRes = await req('批量导入敏感词', 'POST', '/api/admin/content/words/import', {
  token: tAdmin, body: { text: `接口导入词A${ts}\n接口导入词B${ts}\n`, category: 7, level: 1 }
});
check('批量导入新增 2 条', impRes?.data?.added === 2, `added=${impRes?.data?.added}`);

// ---------- 场景策略：管理端开关真实落库 ----------
const pol = await req('读取场景策略', 'GET', '/api/admin/ai-scene-policy', { token: tAdmin });
check('场景策略返回 3 个实验', Array.isArray(pol?.data) && pol.data.length === 3, `len=${pol?.data?.length}`);
await req('保存场景策略(写作默认风格)', 'PUT', '/api/admin/ai-scene-policy', {
  token: tAdmin, body: [{ scene: 'writing', defaultStyle: '科幻风' }]
});
const pol2 = await req('复查场景策略', 'GET', '/api/admin/ai-scene-policy', { token: tAdmin });
const writingStyle = pol2?.data?.find(p => p.scene === 'writing')?.defaultStyle;
check('写作默认风格已落库（证明开关不再是前端演示数据）', writingStyle === '科幻风', `defaultStyle=${writingStyle}`);
await req('恢复写作默认风格', 'PUT', '/api/admin/ai-scene-policy', {
  token: tAdmin, body: [{ scene: 'writing', defaultStyle: '童话风' }]
});
const sp = await req('学生端读取实验参数', 'GET', '/api/ai/scene-policy/quiz', { token: tStudent });
check('学生端参数不含安全开关', sp?.data && !('inputFilter' in sp.data) && !('autoBlock' in sp.data),
  `keys=${Object.keys(sp?.data || {}).join(',')}`);

// ---------- S-01 输入侧拦截：违规指令不进入大模型 ----------
await req('S-01 输入命中禁用词(应 1201)', 'POST', '/api/ai/writing', {
  token: tStudent, body: { topic: '赌博技巧大全', style: '童话风', length: 100 }, expectCode: 1201
});
const logs1 = await req('S-01b 查询输入侧检测日志', 'GET', '/api/admin/content/logs?hitStage=1&page=1&size=5', { token: tAdmin });
const s01 = logs1?.data?.records?.[0];
check('S-01c 拦截日志已落库(hit_stage=1 / action=block)',
  s01?.hitStage === 1 && s01?.action === 'block' && (s01?.inputText || '').includes('赌博'),
  `stage=${s01?.hitStage}, action=${s01?.action}, words=${s01?.hitWords}`);
check('S-01d 被拦截时未调用大模型(outputText 为空)', !s01?.outputText, `outputText=${s01?.outputText ?? 'null'}`);

// ---------- S-02 输出侧过滤：临时关掉答疑场景的输入检测来构造 ----------
await req('S-02a 临时关闭答疑场景输入检测', 'PUT', '/api/admin/ai-scene-policy', {
  token: tAdmin, body: [{ scene: 'chat', inputFilter: 0 }]
});
const s02 = await req('S-02b 提问「赌博」：输入放行、输出应被过滤', 'POST', '/api/ai/chat', {
  token: tStudent, body: { question: '请解释一下「赌博」这个词的含义' }, skipOn: 1101
});
const s02Skipped = s02?.code === 1101;
check('S-02c 返回体带 filtered 标记', s02?.data?.filtered === true,
  `filtered=${s02?.data?.filtered}（若为 false：本次模型输出恰好没出现该禁用词，属模型侧随机性）`, s02Skipped);
check('S-02d 被过滤时返回提示语而非正文', !!s02?.data?.tip, `tip=${s02?.data?.tip}`, s02Skipped);
await req('S-02e 恢复答疑场景输入检测', 'PUT', '/api/admin/ai-scene-policy', {
  token: tAdmin, body: [{ scene: 'chat', inputFilter: 1 }]
});

// ---------- S-03 已知局限：变体绕过 ----------
await req('S-03 插入空格的绕过写法当前不被拦截(已知局限，留待归一化增强)', 'POST', '/api/ai/chat', {
  token: tStudent, body: { question: '赌 博 有什么危害' }, skipOn: 1101
});

// ---------- S-04 权限边界 ----------
await req('S-04 学生访问敏感词库(应 403)', 'GET', '/api/admin/content/words', { token: tStudent, expectCode: 403 });
await req('S-04b 学生访问交互日志(应 403)', 'GET', '/api/admin/content/logs', { token: tStudent, expectCode: 403 });
await req('S-04c 学生访问教师复核列表(应 403)', 'GET', '/api/teacher/content-review/list', { token: tStudent, expectCode: 403 });

// ---------- S-05 教师人工复核闭环 ----------
const cls2 = await req('S-05a 创建复核测试班级', 'POST', '/api/teacher/classes', {
  token: tTeacher, body: { name: '复核测试班' + ts, grade: '四年级', description: '内容安全测试' }
});
const cls2Id = cls2?.data?.id;
if (cls2Id) {
  await req('S-05b 把 student01 加入班级', 'POST', `/api/teacher/classes/${cls2Id}/students`, {
    token: tTeacher, body: { studentId: 3 }
  });
  const tList = await req('S-05c 教师查看待复核列表', 'GET', '/api/teacher/content-review/list?reviewStatus=1&page=1&size=10', { token: tTeacher });
  const target = tList?.data?.records?.find(r => r.userId === 3);
  check('S-05d 教师能看到本班学生的 AI 交互记录', !!target,
    `records=${tList?.data?.records?.length ?? 0}, 含 student01=${!!target}`);
  if (target) {
    await req('S-05e 教师提交复核结论(通过)', 'POST', `/api/teacher/content-review/${target.id}`, {
      token: tTeacher, body: { result: 2, remark: '接口测试：确认无误' }
    });
    const after = await req('S-05f 复核后复查该记录', 'GET', `/api/teacher/content-review/${target.id}`, { token: tTeacher });
    check('S-05g 复核状态已更新为「复核通过」',
      after?.data?.reviewStatus === 2 && after?.data?.reviewerId != null,
      `reviewStatus=${after?.data?.reviewStatus}, reviewerId=${after?.data?.reviewerId}`);
  }
  const pc = await req('S-05h 教师待复核数量', 'GET', '/api/teacher/content-review/pending-count', { token: tTeacher });
  check('S-05i 待复核数量为数字', typeof pc?.data === 'number', `count=${pc?.data}`);
  await req('S-05j 删除复核测试班级', 'DELETE', `/api/teacher/classes/${cls2Id}`, { token: tTeacher });
}

// ---------- S-06 管理员兜底复核 ----------
const pendingLogs = await req('S-06a 管理员查询待复核日志', 'GET', '/api/admin/content/logs?reviewStatus=1&page=1&size=5', { token: tAdmin });
const pLog = pendingLogs?.data?.records?.[0];
if (pLog) {
  await req('S-06b 管理员兜底复核(驳回)', 'POST', `/api/admin/content/logs/${pLog.id}/review`, {
    token: tAdmin, body: { result: 3, remark: '接口测试：驳回' }
  });
  const after = await req('S-06c 复查兜底复核结果', 'GET', `/api/admin/content/logs/${pLog.id}`, { token: tAdmin });
  check('S-06d 复核状态已更新为「复核驳回」', after?.data?.reviewStatus === 3,
    `reviewStatus=${after?.data?.reviewStatus}`);
} else {
  log('S-06b 管理员兜底复核', true, '当前没有待复核记录，跳过');
}

// ---------- S-07 概览统计 ----------
const sum = await req('S-07a 内容安全概览', 'GET', '/api/admin/content/summary?days=7', { token: tAdmin });
check('S-07b 概览含调用量 / 拦截量 / 待复核量',
  sum?.data && sum.data.total != null && sum.data.blocked != null && sum.data.pendingReview != null,
  `total=${sum?.data?.total}, blocked=${sum?.data?.blocked}, pending=${sum?.data?.pendingReview}`);

// ---------- S-08 正常内容不被误杀 ----------
const okRes = await req('S-08 正常科普提问应正常返回', 'POST', '/api/ai/chat', {
  token: tStudent, body: { question: '为什么天空是蓝色的？' }, skipOn: 1101
});
check('S-08b 正常内容 filtered=false（无过度拦截）', okRes?.data?.filtered === false,
  `filtered=${okRes?.data?.filtered}`, okRes?.code === 1101);

// ---------- S-09 参数校验 ----------
await req('S-09 超长提问被参数校验拦下(应 1002)', 'POST', '/api/ai/chat', {
  token: tStudent, body: { question: '啊'.repeat(201) }, expectCode: 1002
});

// ---------- 清理测试词 ----------
const impWords = await req('S-10 查询待清理的导入测试词', 'GET', '/api/admin/content/words?keyword=接口导入词&size=50', { token: tAdmin });
for (const w of (impWords?.data?.records || [])) {
  await quiet('DELETE', `/api/admin/content/words/${w.id}`, tAdmin);
}

// 清理注册的测试用户
if (regId) await req('清理注册的测试用户', 'DELETE', `/api/admin/users/${regId}`, { token: tAdmin });

console.log('\n========== 结果汇总 ==========');
console.log(`✅ 通过 ${pass} 项，❌ 失败 ${fail} 项${skip ? `，⏭️ 跳过 ${skip} 项` : ''}`);
if (skips.length) {
  console.log('\n跳过明细（多为未配置 DeepSeek API Key，不影响其余结论）：');
  skips.forEach(s => console.log(`  ⏭️ ${s}`));
}
if (failures.length) {
  console.log('\n失败明细：');
  failures.forEach(f => console.log(`  ❌ ${f.name}  →  ${f.detail}`));
}
process.exit(fail > 0 ? 1 : 0);
