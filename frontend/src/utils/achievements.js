/**
 * 成就徽章判定。
 *
 * 全部指标来自 `/api/stats/student/my` 的真实聚合值（看课进度 / 编程作品 / AI 写作 / 闯关记录），
 * 不再使用写死的常量 —— 原先首页和个人中心各自维护了一份 `got: true/false` 的假数据，
 * 与真实学习行为完全无关，任何学生看到的徽章状态都一样。
 *
 * ⚠️ 编程作品 / AI 写作 的拆分取自 stats 的 distLabels / distValues，
 *    这两个标签由后端 StudentStatsServiceImpl 定义，改名时需同步此处。
 */
export function buildAchievements(stats) {
  const s = stats || {}
  const dist = {}
  if (Array.isArray(s.distLabels) && Array.isArray(s.distValues)) {
    s.distLabels.forEach((label, i) => { dist[label] = s.distValues[i] || 0 })
  }
  const projects = dist['编程作品'] || 0
  const writings = dist['AI 写作'] || 0
  const quizCount = s.quizCount || 0
  const studyMinutes = s.studyMinutes || 0

  return [
    { emoji: '🚀', name: '初来乍到', desc: '完成注册', got: true },
    { emoji: '📚', name: '学习达人', desc: '累计学习满 5 小时', got: studyMinutes >= 300 },
    { emoji: '🧩', name: '积木大师', desc: '提交 3 个编程作品', got: projects >= 3 },
    { emoji: '✨', name: 'AI 探险家', desc: '完成 3 篇 AI 写作', got: writings >= 3 },
    { emoji: '🏆', name: '闯关王者', desc: '完成 10 次知识闯关', got: quizCount >= 10 }
  ]
}
