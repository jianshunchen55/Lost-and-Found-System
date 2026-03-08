import { createStore, Module } from 'vuex'
// 若构建时报“找不到模块 vuex 或其类型声明”，请运行：
// npm install vuex@next --save
// 或
// yarn add vuex@next
// 并确保 tsconfig.json 中 "moduleResolution": "node"


type User = { 
  username: string; 
  role: string;
  avatar?: string;
  nickname?: string;
  department?: string;
  phone?: string;
  email?: string;
  bio?: string;
}
type AuthState = { token: string; refreshToken: string; user: User | null }
type RootState = { auth: AuthState }

const auth: Module<AuthState, RootState> = {
  namespaced: true,
  state: (): AuthState => {
    const userStr = sessionStorage.getItem('user')
    return {
      token: sessionStorage.getItem('token') || '',
      refreshToken: sessionStorage.getItem('refreshToken') || '',
      user: userStr ? (JSON.parse(userStr) as User) : null
    }
  },
  mutations: {
    setAuth(state, payload: { token: string; refreshToken: string; user: User | null }) {
      state.token = payload.token
      state.refreshToken = payload.refreshToken
      state.user = payload.user
      sessionStorage.setItem('token', state.token)
      sessionStorage.setItem('refreshToken', state.refreshToken)
      sessionStorage.setItem('user', JSON.stringify(state.user))
    },
    setUser(state, user: User) {
      state.user = user
      sessionStorage.setItem('user', JSON.stringify(state.user))
    },
    logout(state) {
      state.token = ''
      state.refreshToken = ''
      state.user = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('refreshToken')
      sessionStorage.removeItem('user')
    }
  }
}

const store = createStore<RootState>({
  modules: { auth }
})

export default store
