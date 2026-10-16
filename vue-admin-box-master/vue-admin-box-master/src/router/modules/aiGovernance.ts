import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'

const route: Route[] = [{
  path: '/ai-governance',
  component: Layout,
  redirect: '/ai-governance/agent-workbench',
  meta: { title: 'AI 全链路治理', icon: 'sfont system-monitor' },
  children: [{
    path: 'workbench',
    component: createNameComponent(() => import('@/views/main/ai-governance/index.vue')),
    // 治理工作台功能已融合进 Agent 页，隐藏菜单入口；路由保留以备比赛演示。
    hideMenu: true,
    meta: { title: '治理工作台（已融合）', icon: 'sfont system-monitor' }
  }, {
    path: 'agent-workbench',
    component: createNameComponent(() => import('@/views/main/ai-governance/agent-workbench.vue')),
    meta: { title: '多源智能体工作台', icon: 'sfont system-data' }
  }, {
    path: 'patrol',
    component: createNameComponent(() => import('@/views/main/patrol/index.vue')),
    meta: { title: '低空巡检指挥台', icon: 'sfont system-monitor' }
  }]
}]

export default route
