import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  clearAuthArtifacts,
  loadStoredProfile,
  persistMemberSession
} from '@/utils/auth'
import {
  googleOAuthLogin as googleOAuthLoginRequest,
  mockLogin as mockLoginRequest,
  passwordLogin as passwordLoginRequest,
  register as registerRequest,
  type MemberPasswordLoginRequest,
  type MemberRegisterRequest,
  type MemberLoginPayload
} from '@/services/member'

export interface MemberProfile {
  id: number | null
  openid: string
  name: string
}

export const useMemberStore = defineStore('member', () => {
  const token = ref(localStorage.getItem('member-token') ?? '')
  const profile = ref<MemberProfile>(loadStoredProfile())

  const isLoggedIn = computed(() => Boolean(token.value))

  function setMember(payload: MemberLoginPayload) {
    token.value = payload.token
    profile.value = {
      id: payload.id,
      openid: payload.openid,
      name: payload.name
    }
    persistMemberSession(payload)
  }

  function clearMember() {
    token.value = ''
    profile.value = {
      id: null,
      openid: '',
      name: ''
    }
    clearAuthArtifacts()
  }

  async function mockLogin(code: string) {
    const payload = await mockLoginRequest(code)
    setMember(payload)
    return payload
  }

  async function passwordLogin(credentials: MemberPasswordLoginRequest) {
    const payload = await passwordLoginRequest(credentials)
    setMember(payload)
    return payload
  }

  async function register(input: MemberRegisterRequest) {
    const payload = await registerRequest(input)
    setMember(payload)
    return payload
  }

  async function googleOAuthLogin(code: string, redirectUri: string) {
    const payload = await googleOAuthLoginRequest(code, redirectUri)
    setMember(payload)
    return payload
  }

  return {
    token,
    profile,
    isLoggedIn,
    setMember,
    clearMember,
    mockLogin,
    passwordLogin,
    register,
    googleOAuthLogin
  }
})
