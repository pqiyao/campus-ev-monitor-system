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
  }]
}]

export default route
