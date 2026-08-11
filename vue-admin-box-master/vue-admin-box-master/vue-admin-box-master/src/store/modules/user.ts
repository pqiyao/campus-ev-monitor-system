import { loginApi, loginOutApi } from '@/api/user'
import { ActionContext } from 'vuex'
import { setUserInfo, removeUserInfo } from '@/utils/auth.js'

export interface UserState {
  token: string
  info: Record<string, any>
}

export type userState = UserState

const state = (): UserState => ({
  token: '',
  info: {},
})

const getters = {
  token(state: UserState) {
    return state.token
  }
}

const mutations = {
  tokenChange(state: UserState, token: string) {
    state.token = token
  },
  infoChange(state: UserState, info: Record<string, any>) {
    state.info = info
  }
}

const actions = {
  login({ commit }: ActionContext<UserState, any>, params: any) {
    return new Promise((resolve, reject) => {
      loginApi(params)
        .then(res => {
          const user = res.data || {}
          const token = user.token || ''
          const { password, token: _token, ...safeUser } = user

          if (!token) {
            reject(new Error('登录成功但未返回token'))
            return
          }

          commit('tokenChange', token)
          commit('infoChange', safeUser)
          setUserInfo(safeUser)

          // Compatibility for legacy pages that read token directly from storage.
          localStorage.setItem('token', token)
          sessionStorage.setItem('token', token)

          resolve(token)
        })
        .catch(err => {
          reject(err)
        })
    })
  },

  loginOut({ commit }: ActionContext<UserState, any>) {
    loginOutApi()
      .catch(() => {
      })
      .finally(() => {
        commit('tokenChange', '')
        commit('infoChange', {})
        removeUserInfo()
        localStorage.removeItem('token')
        sessionStorage.removeItem('token')
        localStorage.removeItem('tabs')
        localStorage.removeItem('vuex')
        sessionStorage.removeItem('vuex')
        location.reload()
      })
  }
}

export default {
  namespaced: true,
  state,
  actions,
  getters,
  mutations
}