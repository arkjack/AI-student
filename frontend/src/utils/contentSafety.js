/* ============================================================
   内容安全模块的枚举与展示映射
   管理端「内容安全」页与教师端「内容复核」页共用，
   取值必须与后端保持一致（见 AiInteractionLog / SensitiveWord 实体注释）。
   ============================================================ */

/** 敏感词分类 */
export const CATEGORY_OPTIONS = [
  { value: 1, label: '涉政' },
  { value: 2, label: '色情' },
  { value: 3, label: '暴力' },
  { value: 4, label: '赌博毒品' },
  { value: 5, label: '迷信诈骗' },
  { value: 6, label: '广告导流' },
  { value: 7, label: '其他' }
]

/** 敏感词级别：1 仅记分 2 替换 3 拦截 */
export const LEVEL_OPTIONS = [
  { value: 1, label: '1 · 仅记分提示' },
  { value: 2, label: '2 · 替换为安全文案' },
  { value: 3, label: '3 · 直接拦截' }
]

/** AI 实验场景 */
export const SCENE_OPTIONS = [
  { value: 'writing', label: '创意写作实验室' },
  { value: 'chat', label: '智能答疑实验室' },
  { value: 'quiz', label: '知识闯关实验室' }
]

/** 处理层级 */
export const STAGE_OPTIONS = [
  { value: 0, label: '通过' },
  { value: 1, label: '输入侧检测' },
  { value: 2, label: '敏感词过滤' },
  { value: 3, label: '适宜性评估' },
  { value: 4, label: '准确性校验' },
  { value: 5, label: '网络降级' }
]

/** 复核状态 */
export const REVIEW_OPTIONS = [
  { value: 0, label: '无需复核' },
  { value: 1, label: '待复核' },
  { value: 2, label: '复核通过' },
  { value: 3, label: '复核驳回' }
]

export const categoryLabel = (v) => CATEGORY_OPTIONS.find((c) => c.value === v)?.label || '其他'

export const categoryClass = (v) => {
  if (v === 2 || v === 4) return 'k-tag--pink'
  if (v === 1 || v === 3) return 'k-tag--orange'
  if (v === 5) return 'k-tag--yellow'
  if (v === 6) return 'k-tag--teal'
  return ''
}

export const levelLabel = (v) => ({ 1: '提示', 2: '替换', 3: '拦截' }[v] || '替换')

export const stageClass = (v) => {
  if (v === 1 || v === 2) return 'k-tag--pink'
  if (v === 3) return 'k-tag--orange'
  if (v === 4) return 'k-tag--yellow'
  if (v === 5) return ''
  return 'k-tag--green'
}

export const reviewClass = (v) => {
  if (v === 1) return 'k-tag--orange'
  if (v === 2) return 'k-tag--green'
  if (v === 3) return 'k-tag--pink'
  return ''
}

export const riskClass = (v) => ((v || 0) >= 60 ? 'is-high' : (v || 0) >= 30 ? 'is-mid' : 'is-low')

/** 去掉 null / undefined / 空串参数，避免后端把空值当成筛选条件 */
export function cleanParams(obj) {
  const out = {}
  Object.keys(obj).forEach((k) => {
    const v = obj[k]
    if (v !== null && v !== undefined && v !== '') out[k] = v
  })
  return out
}
