import axios from 'axios'
import router from '@/router'
import { clearAuthArtifacts, AUTH_STORAGE_KEYS } from '@/utils/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_STORAGE_KEYS.token)
  if (token) {
    config.headers.authentication = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      clearAuthArtifacts()
      const { useMemberStore } = await import('@/stores/member')
      useMemberStore().clearMember()
      if (router.currentRoute.value.path !== '/login') {
        await router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default http
