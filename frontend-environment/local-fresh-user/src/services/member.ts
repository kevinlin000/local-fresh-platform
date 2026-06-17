import { http, unwrap } from '@/services/api'

export interface MemberLoginPayload {
  id: number
  openid: string
  name: string
  token: string
}

export function mockLogin(code: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/login', { code })
  )
}

export function googleOAuthLogin(code: string, redirectUri: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/oauth/google', { code, redirectUri })
  )
}
