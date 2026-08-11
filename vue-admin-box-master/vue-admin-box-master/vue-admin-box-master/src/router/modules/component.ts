import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  {
    path: '/component',
    component: Layout,
    redirect: '/component/button',
    meta: { title: 'message.menu.component.name', icon: 'sfont system-component' },
    // alwayShow: true,
    children: [
      {
        path: 'button',
        component: createNameComponent(() => import('@/views/main/components/button/index.vue')),
        meta: { title: 'message.menu.component.button' },
      },
      // {
      //   path: 'wordEditor',
      //   component: createNameComponent(() => import('@/views/main/components/wordEditor/index.vue')),
      //   meta: { title: 'message.menu.component.wordEditor' },
      // },
    ]
  }
]

export default route

//注册用户管理
