/* ============================================================
   统一动效工具（GSAP）
   - 插件在模块级注册一次（app 级，遵循 gsap-frameworks 规范）
   - 所有动画遵循 gsap-performance：只动 transform/opacity
   - 组件内用 gsap.context(scope) + onUnmounted revert
   ============================================================ */
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export { gsap, ScrollTrigger }

/**
 * 数字滚动（count-up）。
 * 目标元素上放 data-count="目标值" data-decimals="小数位"（可选）。
 * 动画只更新 textContent，无布局抖动。
 */
export function countUpTo(el, { duration = 1.2 } = {}) {
  const target = parseFloat(el.dataset.count)
  if (Number.isNaN(target)) return
  const decimals = Number(el.dataset.decimals || 0)
  const state = { v: 0 }
  gsap.to(state, {
    v: target,
    duration,
    ease: 'power2.out',
    onUpdate: () => {
      el.textContent = state.v.toFixed(decimals)
    }
  })
}

/**
 * 批量数字滚动：对容器内所有 [data-count] 元素逐个 count-up，略作 stagger。
 */
export function countUpGroup(scope, { duration = 1.2, stagger = 0.1 } = {}) {
  const els = gsap.utils.toArray('[data-count]', scope)
  els.forEach((el, i) => {
    gsap.delayedCall(i * stagger, () => countUpTo(el, { duration }))
  })
}

/**
 * prefers-reduced-motion 判断辅助：返回是否应跳过动画。
 */
export function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
