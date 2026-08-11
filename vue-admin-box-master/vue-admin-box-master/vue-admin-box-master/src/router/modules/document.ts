import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  {
    path: '/document',
    component: Layout,
    redirect: '/document/intro',
    meta: { title: 'message.menu.document.name', icon: 'sfont system-document' },
    children: [
      {
        path: 'intro',
        component: createNameComponent(() => import('@/views/main/document/intro.vue')),
        meta: { title: 'message.menu.document.intro' }
      },
      // {
      //   path: 'function',
      //   component: createNameComponent(() => import('@/views/main/document/function.vue')),
      //   meta: { title: 'message.menu.document.function' }
      // },
    ]
  }
]

export default route

//摄像头监控