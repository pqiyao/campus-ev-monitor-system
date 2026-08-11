import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  {
    path: '/directive',
    component: Layout,
    redirect: '/directive/dragable',
    meta: { title: 'message.menu.directive.name', icon: 'sfont system-zidingyi' },
    children: [
      {
        path: 'dragable',
        component: createNameComponent(() => import('@/views/main/directive/dragable.vue')),
        meta: { title: 'message.menu.directive.dragable' }
      },
      // {
      //   path: 'copy',
      //   component: createNameComponent(() => import('@/views/main/directive/copy.vue')),
      //   meta: { title: 'message.menu.directive.copy' }
      // },
    ]
  }
]

export default route

//电动车用户注册
