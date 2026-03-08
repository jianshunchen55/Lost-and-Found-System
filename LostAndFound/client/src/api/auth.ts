import http from './http'

export function login(data: any) {
  return http.post('/auth/login', data)
}
export function register(data: any) {
  return http.post('/auth/register', data)
}
export function forget(data: any) {
  return http.post('/auth/forget', data)
}
