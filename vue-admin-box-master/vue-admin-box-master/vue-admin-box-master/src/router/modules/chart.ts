import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  {
    path: '/echarts',
    component: Layout,
    redirect: '/echarts/bar',
    meta: { title: 'message.menu.echarts.name', icon: 'sfont system-chart' },
    children: [
      {
        path: 'bar',
        component: createNameComponent(() => import('@/views/main/echarts/bar.vue')),
        meta: { title: 'message.menu.echarts.bar' }
      },
      // {
      //   path: 'line',
      //   component: createNameComponent(() => import('@/views/main/echarts/line.vue')),
      //   meta: { title: 'message.menu.echarts.line' }
      // },
    ]
  }
]

export default route
