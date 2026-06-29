import { defineStore } from 'pinia'
import { login as loginApi, userLogout } from '@/api/employee'
import { getToken, getUserInfo, removeToken, removeUserInfo, setToken, setUserInfo } from '@/utils/cookies'

interface LoginPayload {
  username: string
  password: string
}

interface UserState {
  token: string
  name: string
  role: string
}

const savedUser = (() => {
  try {
    return JSON.parse(getUserInfo() || '{}')
  } catch {
    return {}
  }
})()

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken() || '',
    name: savedUser.name || '',
    role: savedUser.role || 'ADMIN'
  }),
  actions: {
    async login(payload: LoginPayload) {
      const response = await loginApi(payload)
      const data = response.data?.data
      const token = data?.token
      if (!token) {
        throw new Error(response.data?.msg || '登入失敗')
      }
      this.token = token
      this.name = data?.name || payload.username
      this.role = data?.role || 'ADMIN'
      setToken(token)
      setUserInfo({ name: this.name, role: this.role })
      return response
    },
    async logout() {
      if (this.token) {
        await userLogout({}).catch(() => undefined)
      }
      this.token = ''
      this.name = ''
      this.role = 'ADMIN'
      removeToken()
      removeUserInfo()
    }
  }
})
