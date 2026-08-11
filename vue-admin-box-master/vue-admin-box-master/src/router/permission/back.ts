/** 动态路由实现基础组件（空框架版，已删除无效依赖） */
/** 引入全局Layout组件（仅保留有效公共组件） */
import Layout from '@/layout/index.vue'
/** 引入带有系统自定义name的组件，方便keep-alive实现 */
import { createNameComponent } from '../createNode'
/** 引入组件映射清单（空框架占位） */
import backConfig from './backConfig'
import { getMenuApi } from '@/api/user'

/** 获取后台模板配置清单（保留原逻辑，无修改） */
const getMenu = async () => {
  const result = await getMenuApi()
  const backRoutes = getComponents(result.data.list)
  return backRoutes
}

/** 循环解析路由组件（核心修改：删除MenuBox，统一用Layout） */
const getComponents = (data: any[], level = 1) => {
  const newData: any[] = data.map((item) => {
    if (item.children) {
      // 所有层级路由统一使用Layout公共布局，避免MenuBox组件缺失
      return {
        ...item,
        component: Layout,
        children: getComponents(item.children, level + 1)
      }
    } else {
      // 后端组件标识解析逻辑（保留原框架，后续自行适配）
      const [first, end] = item.component.split('_')
      const component = backConfig[first][end]
      return {
        ...item,
        component,
      }
    }
  })
  return newData
}

export default getMenu