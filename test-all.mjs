/* 全量功能测试：覆盖认证/权限/学生端/教师端/管理端/AI
   运行：node test-all.mjs
   后端需运行在 127.0.0.1:8080 */
const BASE = 'http://127.0.0.1:8080';

let pass = 0, fail = 0;
const failures = [];
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

async function req(name, method, path, { token, body, expectCode = 0, dataKey } = {}) {
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
const aiCfg = await req('保存 AI 配置(空key保留原值)', 'PUT', '/api/admin/ai-config', { token: tAdmin, body: { apiKey: '', baseUrl: 'https://api.deepseek.com', model: 'deepseek-chat', timeoutSec: 60, maxConcurrency: 3, rateLimitPerMin: 20, contentFilter: 1 } });

console.log('\n========== ⑤ AI 能力 ==========');
await req('AI 创意写作', 'POST', '/api/ai/writing', { token: tStudent, body: { topic: '会飞的小狗', style: '童话', length: 200 } });
await req('AI 智能答疑', 'POST', '/api/ai/chat', { token: tStudent, body: { question: '为什么天空是蓝色的？' } });
await req('AI 知识闯关出题', 'POST', '/api/ai/quiz', { token: tTeacher, body: { level: 1, count: 3 } });

// 清理注册的测试用户
if (regId) await req('清理注册的测试用户', 'DELETE', `/api/admin/users/${regId}`, { token: tAdmin });

console.log('\n========== 结果汇总 ==========');
console.log(`✅ 通过 ${pass} 项，❌ 失败 ${fail} 项`);
if (failures.length) {
  console.log('\n失败明细：');
  failures.forEach(f => console.log(`  ❌ ${f.name}  →  ${f.detail}`));
}
process.exit(fail > 0 ? 1 : 0);
