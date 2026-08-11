import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  {
    path: '/pages',
    component: Layout,
    redirect: '/pages/crudTable',
    meta: { title: 'message.menu.page.name', icon: 'sfont system-page' },
    // alwayShow: true,
    children: [
      {
        path: 'crudTable',
        component: createNameComponent(() => import('@/views/main/pages/crudTable/index.vue')),
        meta: { title: 'message.menu.page.crudTable', cache: false, roles: ['admin', 'editor'] }
      },
      // {
      //   path: 'categoryTable',
      //   component: createNameComponent(() => import('@/views/main/pages/categoryTable/index.vue')),
      //   meta: { title: 'message.menu.page.categoryTable', cache: true, roles: ['admin'] }
      // },
    ]
  }
]

export default route

//消息管理