import type { Route } from '../index.type'
import Layout from '@/layout/index.vue'
import { createNameComponent } from '../createNode'
const route: Route[] = [
  // {
  //   path: '/systemManage',
  //   component: Layout,
  //   redirect: '/systemManage/menu',
  //   meta: { title: 'message.menu.systemManage.name', icon: 'sfont system-xitongzhuangtai' },
  //   // alwayShow: true,
  //   children: [
  //     {
  //       path: 'menu',
  //       component: createNameComponent(() => import('@/views/main/systemManage/menu/index.vue')),
  //       meta: { title: 'message.menu.systemManage.menu' }
  //     }
  //   ]
  // }
]

export default route