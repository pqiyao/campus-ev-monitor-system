import { createNameComponent } from '../createNode'

const dashboard = {
  dashboard: createNameComponent(() => import('@/views/main/dashboard/index.vue')),
}

const document = {
  intro: createNameComponent(() => import('@/views/main/document/intro.vue')),
}

const component = {
  button: createNameComponent(() => import('@/views/main/components/button/index.vue')),
}

const pages = {
  crudTable: createNameComponent(() => import('@/views/main/pages/crudTable/index.vue')),
}

// This project does not use demo nested-menu pages.
const menu = {}

const directive = {
  dragable: createNameComponent(() => import('@/views/main/directive/dragable.vue')),
}

const echarts = {
  bar: createNameComponent(() => import('@/views/main/echarts/bar.vue')),
}

const systemManage = {}

const print = {
  print: createNameComponent(() => import('@/views/main/print/index.vue')),
}

const community = {
  qq: createNameComponent(() => import('@/views/main/community/qq.vue')),
  userManage: createNameComponent(() => import('@/views/main/community/components/card.vue')),
  site: createNameComponent(() => import('@/views/main/community/site.vue')),
  notification: createNameComponent(() => import('@/views/main/community/notification.vue')),
  licensePoints: createNameComponent(() => import('@/views/main/community/licensePoints.vue')),
}

const tab = {}

const allRoutes = {
  dashboard,
  document,
  component,
  pages,
  menu,
  directive,
  echarts,
  systemManage,
  print,
  community,
  tab,
}

export default allRoutes