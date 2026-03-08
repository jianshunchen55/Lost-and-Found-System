// @ts-ignore
import axios, { AxiosInstance } from 'axios'
import store from '../store'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config: any) => {
  const token = (store.state as any).auth?.token
  if (token) {
    config.headers = config.headers || {}
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp: any) => resp,
  async (error: any) => {
    // Skip 401 handling for login request to allow component to handle invalid credentials
    if (error.config && error.config.url && error.config.url.includes('/auth/login')) {
      return Promise.reject(error)
    }

    if (error.response && error.response.status === 401) {
      const rt = (store.state as any).auth?.refreshToken
      if (rt) {
        try {
          const r = await axios.post('/api/auth/refresh', null, { params: { refreshToken: rt } })
          store.commit('auth/setAuth', { token: r.data.accessToken, refreshToken: rt, user: (store.state as any).auth.user })
          error.config.headers['Authorization'] = `Bearer ${r.data.accessToken}`
          return http.request(error.config)
        } catch (e) {
          store.commit('auth/logout')
          location.href = '/login'
        }
      } else {
        store.commit('auth/logout')
        location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http
