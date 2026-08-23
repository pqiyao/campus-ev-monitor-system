import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'

const route: Route[] = [{
  path: '/ai-governance',
  component: Layout,
  redirect: '/ai-governance/workbench',
  meta: { title: 'AI 全链路治理', icon: 'sfont system-monitor' },
  children: [{
    path: 'workbench',
    component: createNameComponent(() => import('@/views/main/ai-governance/index.vue')),
    meta: { title: '治理工作台', icon: 'sfont system-monitor' }
  }, {
    path: 'intelligence',
    component: createNameComponent(() => import('@/views/main/ai-governance/intelligence.vue')),
    meta: { title: 'AI 分析助手', icon: 'sfont system-data' }
  }, {
    path: 'patrol',
    component: createNameComponent(() => import('@/views/main/patrol/index.vue')),
    // 保留直达调试页，但不把它暴露为比赛主菜单入口；主流程从原大屏进入。
    hideMenu: true,
    meta: { title: '低空巡检指挥台', icon: 'sfont system-monitor' }
  }]
}]

export default route
