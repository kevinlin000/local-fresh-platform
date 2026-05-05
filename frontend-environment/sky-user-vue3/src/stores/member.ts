import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface MemberProfile {
  id: number | null
  openid: string
  name: string
}

export const useMemberStore = defineStore('member', () => {
  const token = ref(localStorage.getItem('member-token') ?? '')
  const profile = ref<MemberProfile>({
    id: Number(localStorage.getItem('member-id') ?? 0) || null,
    openid: localStorage.getItem('member-openid') ?? '',
    name: localStorage.getItem('member-name') ?? ''
  })

  const isLoggedIn = computed(() => Boolean(token.value))

  function setMember(payload: { token: string; id: number; openid: string; name: string }) {
    token.value = payload.token
    profile.value = {
      id: payload.id,
      openid: payload.openid,
      name: payload.name
    }
    localStorage.setItem('member-token', payload.token)
    localStorage.setItem('member-id', String(payload.id))
    localStorage.setItem('member-openid', payload.openid)
    localStorage.setItem('member-name', payload.name)
  }

  function clearMember() {
    token.value = ''
    profile.value = {
      id: null,
      openid: '',
      name: ''
    }
    localStorage.removeItem('member-token')
    localStorage.removeItem('member-id')
    localStorage.removeItem('member-openid')
    localStorage.removeItem('member-name')
  }

  return {
    token,
    profile,
    isLoggedIn,
    setMember,
    clearMember
  }
})
