import { defineStore } from 'pinia'
import { login as loginApi, userLogout } from '@/api/employee'
import { getToken, removeToken, setToken } from '@/utils/cookies'

interface LoginPayload {
  username: string
  password: string
}

interface UserState {
  token: string
  name: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken() || '',
    name: ''
  }),
  actions: {
    async login(payload: LoginPayload) {
      const response = await loginApi(payload)
      const token = response.data?.data?.token
      if (!token) {
        throw new Error(response.data?.msg || '登入失敗')
      }
      this.token = token
      this.name = response.data?.data?.name || payload.username
      setToken(token)
      return response
    },
    async logout() {
      if (this.token) {
        await userLogout({}).catch(() => undefined)
      }
      this.token = ''
      this.name = ''
      removeToken()
    }
  }
})
