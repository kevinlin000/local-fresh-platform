import axios from 'axios'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('member-token')
  if (token) {
    config.headers.authentication = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('member-token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default http
