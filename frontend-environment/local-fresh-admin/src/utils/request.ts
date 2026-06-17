import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken } from '@/utils/cookies'

const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || '/api',
  timeout: 600000
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.token = token
  }
  return config
})

service.interceptors.response.use(
  response => {
    if (response.data?.code === 0) {
      ElMessage.error(response.data?.msg || '請求失敗')
    }
    return response
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      removeToken()
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.msg || error.message || '系統錯誤')
    return Promise.reject(error)
  }
)

export default service
